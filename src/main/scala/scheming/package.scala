import java.util.UUID

import magnolia._
import scheming.Tables._
import scheming.Tables.profile.api._
import zio._

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros

/**
  * TODO: I kinda just made these ADTs up as I went along, switching them up as I needed something. Type is used
  *  for deriving Schema definitions while Value is used for inserting the records of the objects a schema defines.
  *  These could be refactored to be both better named and more useful. But it works for the examples for now :)
  */
sealed trait Type
object Type {
  case object Int     extends Type
  case object String  extends Type
  case object Boolean extends Type

  case class Object(source: String, name: String, fields: List[Object.Field], relationships: List[Object.Relationship])
      extends Type
  case class Reference(name: String) extends Type

  object Object {
    case class Field(name: String, tpe: Type)
    case class Relationship(name: String, obj: Type) extends Type
  }
}

sealed trait Value
object Value {
  case class Primitive(value: String, tpe: String) extends Value
  case class IdentifiedPrimitive(source: String, obj: String, objId: String, field: String, primitive: Primitive)
      extends Value
}

package object scheming {

  /**
    * Here's the trait! Get type is used to generate schemas with `createSchema` and getValues is used to insert
    * records with `insertRecords`
    */
  trait Schemable[A] {
    def getType: Type
    def getValues(a: A): List[Value]
  }

  /**
    * Some default implementations for Ints and Strings
    */
  object Schemable {
    implicit val schemableInt: Schemable[Int] = new Schemable[Int] {
      override def getType: Type = Type.Int

      override def getValues(a: Int): List[Value] = List(Value.Primitive(a.toString, "Int"))
    }
    implicit val schemableString: Schemable[String] = new Schemable[String] {
      override def getType: Type = Type.String

      override def getValues(a: String): List[Value] = List(Value.Primitive(a, "String"))
    }
    implicit val schemableBoolean: Schemable[Boolean] = new Schemable[Boolean] {
      override def getType: Type = Type.Boolean

      override def getValues(a: Boolean): List[Value] = List(Value.Primitive(a.toString, "Boolean"))
    }

    def apply[A: Schemable]: Schemable[A] = implicitly[Schemable[A]]
  }

  case class Source(name: String) extends StaticAnnotation

  /**
    * This is the exciting bit! We use `magnolia` to create a Schemable instance for any case class!
    * I've yet to define one for sealed traits, but we could easily do that and then store the resultant types
    * as a picklist-style schema.
    */
  object SchemableDerivation {
    type Typeclass[T] = Schemable[T]

    def combine[T](ctx: CaseClass[Schemable, T]): Schemable[T] = new Schemable[T] {
      override def getType: Type = {
        val labelsAndTypes = ctx.parameters.map(p => (p.label, p.typeclass.getType))
        val (relationships, fields) = labelsAndTypes.partitionMap {
          case (label, value) =>
            value match {
              case obj: Type.Object.Relationship => Left(obj)
              case other                         => Right(Type.Object.Field(StringUtils.camelToSnakeCase(label), value))
            }
        }

        val source = ctx.annotations.collectFirst { case Source(name) => name }.get
        Type.Object(source, ctx.typeName.short, fields.toList, relationships.toList)
      }

      override def getValues(a: T): List[Value] = {
        val source = ctx.annotations.collectFirst { case Source(name) => name }.get
        val objId  = UUID.randomUUID().toString

        ctx.parameters
          .map(p => (p.label, p.typeclass.getValues(p.dereference(a))))
          .flatMap {
            case (label, value) =>
              value.map {
                case prim @ Value.Primitive(_, _) =>
                  Value.IdentifiedPrimitive(source, ctx.typeName.short.toString, objId, label, prim)
                case identified: Value.IdentifiedPrimitive => identified
              }
          }
          .toList
      }
    }

    implicit def gen[T]: Schemable[T] = macro Magnolia.gen[T]
  }

  val url             = "jdbc:postgresql://localhost:5432/experiment"
  lazy private val db = Database.forURL(url)

  /**
    * Messing around with some types that can be used for specifying particular relationship shapes.
    * Very much a WIP. Holding off on making any of this too nice until I know what would actually be useful.
    */
  case class HasOne[A](item: A)
  case class HasMany[A](items: List[A])
  object HasMany {
    implicit def schemableHasMany[A: Schemable]: Schemable[HasMany[A]] = new Schemable[HasMany[A]] {
      override def getType: Type = Type.Object.Relationship("HasMany", Schemable[A].getType)

      override def getValues(a: HasMany[A]): List[Value] = a.items.flatMap(Schemable[A].getValues)
    }
  }
  object HasOne {
    implicit def schemableHasOne[A: Schemable]: Schemable[HasOne[A]] = new Schemable[HasOne[A]] {
      override def getType: Type = Type.Object.Relationship("HasOne", Schemable[A].getType)

      override def getValues(a: HasOne[A]): List[Value] = Schemable[A].getValues(a.item)
    }
  }

  final def runDb[A](io: DBIOAction[A, NoStream, Nothing]): Task[A] = ZIO.fromFuture(_ => db.run(io))

  /**
    * TODO: Make this less ugly. Still getting used to Slick and was plowing ahead towards something a working POC
    * We first must gather all the ids for the related schema ids (For SchemaSource, SchemaObject, and SchemaField).
    *  Then we create the objects, since we'll need their ids for creating related fields.
    *  Finally, we create the fields themselves.
    */
  def insertRecords[A: Schemable](schemables: A*): Task[Unit] = {
    val values = schemables.flatMap(Schemable[A].getValues(_))
    val sourceNames = values.collect {
      case identified: Value.IdentifiedPrimitive => identified.source
    }.toSet
    val objectsToCreate = values.collect {
      case Value.IdentifiedPrimitive(source, obj, objId, _, _) => objId -> (source, obj)
    }.toMap
    val fieldsToCreate: Seq[Value.IdentifiedPrimitive] = values.collect {
      case field: Value.IdentifiedPrimitive => field
    }

    val getSources =
      SchemaField
        .join(SchemaObject)
        .on(_.dataObjectId === _.id)
        .join(SchemaSource)
        .on(_._2.schemaSourceId === _.id)
        .map { case ((field, obj), source) => (field, obj, source) }
        .filter { case (_, _, source) => source.name inSet sourceNames }
    for {
      result <- runDb(getSources.result)
      objectMap = result.map { case (_, obj, source) => (source.name, obj.name) -> obj.id }.toMap
      createObjects = objectsToCreate.map {
        case (_, sourceObjectTuple) if objectMap.contains(sourceObjectTuple) =>
          val schemaObjectId = objectMap.get(sourceObjectTuple).flatten.get
          DataObjectRow(schemaObjectId)
      }.toSeq
      createObjectsQuery = DataObject.returning(DataObject.map(_.id)) ++= createObjects
      createdObjects <- runDb(createObjectsQuery)
      createdObjectIdMap = objectsToCreate.keys.zip(createdObjects).toMap
      fieldMap           = result.map { case (field, obj, source) => (source.name, obj.name, field.name) -> field.id }.toMap
      createFields = fieldsToCreate.map { field =>
        val schemaFieldId = fieldMap((field.source, field.obj, field.field)).get
        val objectId      = createdObjectIdMap(field.objId)
        DataFieldRow(schemaFieldId, field.primitive.value, objectId)
      }
      createFieldsQuery = DataField.returning(DataField.map(_.id)) ++= createFields
      _ <- runDb(createFieldsQuery)
    } yield ()
  }

  private object Schema {
    sealed trait Action
    case class Source(name: String)                                                                     extends Action
    case class Object(source: String, name: String)                                                     extends Action
    case class Field(source: String, obj: String, name: String, tpe: String)                            extends Action
    case class Join(source: String, fromObj: String, fromField: String, toObj: String, toField: String) extends Action
  }

  /**
    * TODO: Refactor this! Same excuse as above :)
    * This traverses our nested type into a flat structure of every db action that must be taken to create the schema.
    */
  private def getSchemaActions(obj: Type.Object): Set[Schema.Action] = {
    val sources = List(Schema.Source(obj.source))
    val objects = List(Schema.Object(obj.source, obj.name))
    val fields  = obj.fields.map(f => Schema.Field(obj.source, obj.name, f.name, f.tpe.toString))
    val joins = obj.relationships.filter(_.name == "HasMany").flatMap { r =>
      val joinedObject = r.obj.asInstanceOf[Type.Object]
      val joinField    = StringUtils.camelToSnakeCase(s"${obj.name}Id")
      List(Schema.Join(obj.source, joinedObject.name, joinField, obj.name, "id"),
           Schema.Field(obj.source, joinedObject.name, joinField, "String"))
    }
    val joinTables = obj.relationships.filter(_.name == "HasOne").flatMap { r =>
      val joinedObject  = r.obj.asInstanceOf[Type.Object]
      val joinTableName = s"${obj.name}_${joinedObject.name}_JoinTable"
      val fromFieldName = StringUtils.camelToSnakeCase(obj.name + "Id")
      val toFieldName   = StringUtils.camelToSnakeCase(joinedObject.name + "Id")
      List(
        Schema.Object(obj.source, joinTableName),
        Schema.Field(obj.source, joinTableName, fromFieldName, "String"),
        Schema.Field(obj.source, joinTableName, toFieldName, "String"),
        Schema.Join(obj.source, joinTableName, fromFieldName, obj.name, "id"),
        Schema.Join(obj.source, joinTableName, toFieldName, joinedObject.name, "id"),
      )
    }
    val childActions = obj.relationships.map(_.obj.asInstanceOf[Type.Object]).flatMap(getSchemaActions)
    (sources ++ objects ++ fields ++ joins ++ joinTables ++ childActions).toSet
  }

  /**
    * This runs the actions generated in the above function.
    */
  private def runActions(actions: Set[Schema.Action]) = {
    val sources = actions.collect { case source: Schema.Source => source }
    val objects = actions.collect { case obj: Schema.Object    => obj }
    val fields  = actions.collect { case field: Schema.Field   => field }
    val joins   = actions.collect { case join: Schema.Join     => join }

    val sourceInserts = SchemaSource ++= sources.map(source => SchemaSourceRow(source.name))
    def objectInserts(sourceMap: Map[String, Int]) =
      SchemaObject ++= objects.map(obj => SchemaObjectRow(obj.name, sourceMap(obj.source)))
    def getFieldsWithObjects(fieldIds: Set[Int]) =
      SchemaField.filter(_.id inSet fieldIds).join(SchemaObject).on(_.dataObjectId === _.id)
    def fieldInserts(objectMap: Map[String, Int]) =
      SchemaField.returning(SchemaField.map(_.id)) ++= fields.map(field =>
        SchemaFieldRow(objectMap(field.obj), field.name, field.tpe))
    def joinInserts(fieldMap: Map[(String, String, String), Int]) =
      SchemaJoin ++= joins.map(
        join =>
          SchemaJoinRow(fieldMap((join.source, join.fromObj, join.fromField)),
                        fieldMap((join.source, join.toObj, join.toField)),
        ))

    for {
      _          <- runDb(SchemaSource.filter(_.name inSet sources.map(_.name)).delete)
      _          <- runDb(sourceInserts)
      allSources <- runDb(SchemaSource.filter(_.name inSet sources.map(_.name)).result)
      sourceMap = allSources.map(s => (s.name, s.id.get)).toMap

      _ <- runDb(objectInserts(sourceMap))
      allObjects <- runDb(
        SchemaObject
          .filter(o => (o.name inSet objects.map(_.name)) && (o.schemaSourceId inSet sourceMap.values))
          .result)
      objectMap = allObjects.map(o => (o.name, o.id.get)).toMap
      insertedFields <- runDb(fieldInserts(objectMap))
      fields         <- runDb(getFieldsWithObjects(insertedFields.toSet).result)
      fieldMap = fields.map { case (field, obj) => (sources.head.name, obj.name, field.name) -> field.id.get }.toMap
      _ <- runDb(joinInserts(fieldMap))
    } yield ()
  }

  /**
    * Forgets the implementation details and create the dang schema.
    */
  def createSchema[A](implicit schemable: Schemable[A]): Task[Unit] = {
    val schema  = schemable.getType
    val actions = getSchemaActions(schema.asInstanceOf[Type.Object])
    runActions(actions)
  }
}
