package scheming
import caliban.GraphQL._
import caliban.schema.GenericSchema
import caliban._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import scheming.Tables._
import scheming.Tables.profile.api._
import zio._
import zio.interop.catz._
import zquery._

object DataSources {
  private val url     = "jdbc:postgresql://localhost:5432/experiment"
  lazy private val db = Database.forURL(url)

  type Query[A] = ZQuery[Any, Nothing, A]

  case class Field(id: Int, name: String, `type`: String, joins: Query[List[Field]])
  case class Object(id: Int, source: String, name: String, fields: Query[List[Field]])
  case class Join(id: Int, fromId: Int, toId: Int)

  final case object AllJoins extends Request[Nothing, List[Join]]

  final case class GetJoins(fieldId: Int) extends Request[Nothing, List[Field]]
  def FieldJoinsDataSource[A]: DataSource[Any, GetJoins] =
    DataSource.fromFunctionBatchedM("FieldJoinsDataSource") { requests =>
      val fieldIds = requests.map(_.fieldId)

      val query = SchemaField
        .join(SchemaJoin)
        .on(_.id === _.toId)
        .filter(_._2.fromId inSet fieldIds)
        .result

      for {
        fieldRows <- ZIO.fromFuture(_ => db.run(query)).map(_.toList)
        fieldsByJoinIds = fieldRows.groupMap(_._2.fromId) {
          case (field, _) =>
            Field(field.id.get, field.name, field.`type`, getJoins(field.id.get))
        }
      } yield requests.map(request => fieldsByJoinIds.getOrElse(request.fieldId, List.empty))
    }

  final case class GetFields(objectId: Int) extends Request[Nothing, List[Field]]
  def FieldsDataSource[A]: DataSource[Any, GetFields] =
    DataSource.fromFunctionBatchedM("FieldsDataSource") { requests =>
      val objectIds = requests.map(_.objectId)
      val query     = SchemaField.filter(_.dataObjectId inSet objectIds).result

      for {
        fieldRows <- ZIO.fromFuture(_ => db.run(query)).map(_.toList)
        fieldsByObjectIds = fieldRows.groupMap(_.dataObjectId) { field =>
          Field(field.id.get, field.name, field.`type`, getJoins(field.id.get))
        }
      } yield requests.map(request => fieldsByObjectIds.getOrElse(request.objectId, List.empty))
    }

  def getJoins(fieldId: Int): Query[List[Field]] =
    ZQuery.fromRequest(GetJoins(fieldId))(FieldJoinsDataSource)

  def getFields(objectId: Int): Query[List[Field]] =
    ZQuery.fromRequest(GetFields(objectId))(FieldsDataSource)

  case object GetObjects extends Request[Nothing, List[Object]]
  val ObjectDataSource: DataSource[Any, GetObjects.type] =
    DataSource.fromFunctionM("ObjectsDataSource") { _ =>
      for {
        objects <- ZIO
          .fromFuture(
            _ =>
              db.run(
                SchemaObject
                  .join(SchemaSource)
                  .on(_.schemaSourceId === _.id)
                  .result))
      } yield
        objects.toList.map {
          case (obj, source) =>
            Object(obj.id.get, source.name, obj.name, getFields(obj.id.get))
        }
    }

  def getObjects: Query[List[Object]] =
    ZQuery.fromRequest(GetObjects)(ObjectDataSource)

  case class ObjectTypeaheadArgs(query: String)
  case class ObjectTypeaheadResult(name: String, id: Int)
  def objectTypeahead(args: ObjectTypeaheadArgs): Task[List[ObjectTypeaheadResult]] =
    ZIO
      .fromFuture(
        _ =>
          db.run(
            SchemaObject
              .filter(_.name.ilike(s"%${args.query}%"))
              .map(obj => (obj.name, obj.id))
              .result))
      .map(_.toList.map(obj => ObjectTypeaheadResult(obj._1, obj._2)))

  case class Queries(objects: Query[List[Object]],
                     objectsTypeahead: ObjectTypeaheadArgs => Task[List[ObjectTypeaheadResult]],
                     allJoins: Task[List[Join]])

  private val joinsQuery = SchemaJoin.map(j => (j.id, j.fromId, j.toId)).result
  private val getAllJoins =
    ZIO
      .fromFuture(_ => db.run(joinsQuery))
      .map(_.map(j => Join(j._1, j._2, j._3)).toList)

  val queries: Queries = Queries(getObjects, objectTypeahead, getAllJoins)

  def addJoin(joinArgs: JoinArgs): Task[Int] =
    ZIO.fromFuture(_ => db.run(SchemaJoin += SchemaJoinRow(joinArgs.fromFieldId, joinArgs.toFieldId)))

  case class JoinArgs(fromFieldId: Int, toFieldId: Int)
  case class Mutations(joinFields: JoinArgs => Task[Int])

  val mutations: Mutations = Mutations(addJoin)
}

object Server extends CatsApp with GenericSchema[ZEnv] {
  type ExampleTask[A] = RIO[ZEnv, A]
  val api: GraphQL[ZEnv] = graphQL(RootResolver(DataSources.queries, DataSources.mutations))

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    (for {
      interpreter <- api.interpreter
      _ <- BlazeServerBuilder[ExampleTask]
        .bindHttp(8088, "localhost")
        .withWebSockets(true)
        .withHttpApp(
          Router[ExampleTask](
            "/api/graphql" -> CORS(Http4sAdapter.makeHttpService(interpreter)),
            "/ws/graphql"  -> CORS(Http4sAdapter.makeWebSocketService(interpreter)),
          ).orNotFound
        )
        .resource
        .toManaged
        .useForever
    } yield 0)
      .catchAll(err => zio.console.putStrLn(err.toString).as(1))
}
