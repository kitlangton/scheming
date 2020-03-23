package scheming
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = scheming.DatabaseDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: scheming.DatabaseDriver
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(
    DataField.schema,
    DataObject.schema,
    SchemaField.schema,
    SchemaJoin.schema,
    SchemaObject.schema,
    SchemaSource.schema
  ).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table DataField
    *  @param schemaFieldId Database column schema_field_id SqlType(int4)
    *  @param value Database column value SqlType(text)
    *  @param dataObjectId Database column data_object_id SqlType(int4)
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey */
  case class DataFieldRow(schemaFieldId: Int, value: String, dataObjectId: Int, id: Option[Int] = None)

  /** GetResult implicit for fetching DataFieldRow objects using plain SQL queries */
  implicit def GetResultDataFieldRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[Int]]): GR[DataFieldRow] = GR {
    prs =>
      import prs._
      val r = (<<?[Int], <<[Int], <<[String], <<[Int])
      import r._
      DataFieldRow.tupled((_2, _3, _4, _1)) // putting AutoInc last
  }

  /** Table description of table data_field. Objects of this class serve as prototypes for rows in queries. */
  class DataField(_tableTag: Tag) extends profile.api.Table[DataFieldRow](_tableTag, "data_field") {
    def * = (schemaFieldId, value, dataObjectId, Rep.Some(id)) <> (DataFieldRow.tupled, DataFieldRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? =
      ((Rep.Some(schemaFieldId), Rep.Some(value), Rep.Some(dataObjectId), Rep.Some(id))).shaped.<>({ r =>
        import r._; _1.map(_ => DataFieldRow.tupled((_1.get, _2.get, _3.get, _4)))
      }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column schema_field_id SqlType(int4) */
    val schemaFieldId: Rep[Int] = column[Int]("schema_field_id")

    /** Database column value SqlType(text) */
    val value: Rep[String] = column[String]("value")

    /** Database column data_object_id SqlType(int4) */
    val dataObjectId: Rep[Int] = column[Int]("data_object_id")

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

    /** Foreign key referencing DataObject (database name data_field_data_object_id_fk_2) */
    lazy val dataObjectFk = foreignKey("data_field_data_object_id_fk_2", dataObjectId, DataObject)(
      r => r.id,
      onUpdate = ForeignKeyAction.NoAction,
      onDelete = ForeignKeyAction.Cascade)

    /** Foreign key referencing SchemaField (database name data_field_schema_field_id_fk) */
    lazy val schemaFieldFk = foreignKey("data_field_schema_field_id_fk", schemaFieldId, SchemaField)(
      r => r.id,
      onUpdate = ForeignKeyAction.NoAction,
      onDelete = ForeignKeyAction.Cascade)
  }

  /** Collection-like TableQuery object for table DataField */
  lazy val DataField = new TableQuery(tag => new DataField(tag))

  /** Entity class storing rows of table DataObject
    *  @param schemaObjectId Database column schema_object_id SqlType(int4)
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey */
  case class DataObjectRow(schemaObjectId: Int, id: Option[Int] = None)

  /** GetResult implicit for fetching DataObjectRow objects using plain SQL queries */
  implicit def GetResultDataObjectRow(implicit e0: GR[Int], e1: GR[Option[Int]]): GR[DataObjectRow] = GR { prs =>
    import prs._
    val r = (<<?[Int], <<[Int])
    import r._
    DataObjectRow.tupled((_2, _1)) // putting AutoInc last
  }

  /** Table description of table data_object. Objects of this class serve as prototypes for rows in queries. */
  class DataObject(_tableTag: Tag) extends profile.api.Table[DataObjectRow](_tableTag, "data_object") {
    def * = (schemaObjectId, Rep.Some(id)) <> (DataObjectRow.tupled, DataObjectRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? =
      ((Rep.Some(schemaObjectId), Rep.Some(id))).shaped.<>({ r =>
        import r._; _1.map(_ => DataObjectRow.tupled((_1.get, _2)))
      }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column schema_object_id SqlType(int4) */
    val schemaObjectId: Rep[Int] = column[Int]("schema_object_id")

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

    /** Foreign key referencing SchemaObject (database name data_object_schema_object_id_fk) */
    lazy val schemaObjectFk = foreignKey("data_object_schema_object_id_fk", schemaObjectId, SchemaObject)(
      r => r.id,
      onUpdate = ForeignKeyAction.NoAction,
      onDelete = ForeignKeyAction.Cascade)
  }

  /** Collection-like TableQuery object for table DataObject */
  lazy val DataObject = new TableQuery(tag => new DataObject(tag))

  /** Entity class storing rows of table SchemaField
    *  @param dataObjectId Database column data_object_id SqlType(int4)
    *  @param name Database column name SqlType(varchar), Length(256,true)
    *  @param `type` Database column type SqlType(varchar), Length(256,true)
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey */
  case class SchemaFieldRow(dataObjectId: Int, name: String, `type`: String, id: Option[Int] = None)

  /** GetResult implicit for fetching SchemaFieldRow objects using plain SQL queries */
  implicit def GetResultSchemaFieldRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[Int]]): GR[SchemaFieldRow] =
    GR { prs =>
      import prs._
      val r = (<<?[Int], <<[Int], <<[String], <<[String])
      import r._
      SchemaFieldRow.tupled((_2, _3, _4, _1)) // putting AutoInc last
    }

  /** Table description of table schema_field. Objects of this class serve as prototypes for rows in queries.
    *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class SchemaField(_tableTag: Tag) extends profile.api.Table[SchemaFieldRow](_tableTag, "schema_field") {
    def * = (dataObjectId, name, `type`, Rep.Some(id)) <> (SchemaFieldRow.tupled, SchemaFieldRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? =
      ((Rep.Some(dataObjectId), Rep.Some(name), Rep.Some(`type`), Rep.Some(id))).shaped.<>({ r =>
        import r._; _1.map(_ => SchemaFieldRow.tupled((_1.get, _2.get, _3.get, _4)))
      }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column data_object_id SqlType(int4) */
    val dataObjectId: Rep[Int] = column[Int]("data_object_id")

    /** Database column name SqlType(varchar), Length(256,true) */
    val name: Rep[String] = column[String]("name", O.Length(256, varying = true))

    /** Database column type SqlType(varchar), Length(256,true)
      *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[String] = column[String]("type", O.Length(256, varying = true))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

    /** Foreign key referencing SchemaObject (database name data_field_data_object_id_fk) */
    lazy val schemaObjectFk = foreignKey("data_field_data_object_id_fk", dataObjectId, SchemaObject)(
      r => r.id,
      onUpdate = ForeignKeyAction.NoAction,
      onDelete = ForeignKeyAction.Cascade)
  }

  /** Collection-like TableQuery object for table SchemaField */
  lazy val SchemaField = new TableQuery(tag => new SchemaField(tag))

  /** Entity class storing rows of table SchemaJoin
    *  @param fromId Database column from_id SqlType(int4)
    *  @param toId Database column to_id SqlType(int4)
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey */
  case class SchemaJoinRow(fromId: Int, toId: Int, id: Option[Int] = None)

  /** GetResult implicit for fetching SchemaJoinRow objects using plain SQL queries */
  implicit def GetResultSchemaJoinRow(implicit e0: GR[Int], e1: GR[Option[Int]]): GR[SchemaJoinRow] = GR { prs =>
    import prs._
    val r = (<<[Int], <<[Int], <<?[Int])
    import r._
    SchemaJoinRow.tupled((_1, _2, _3)) // putting AutoInc last
  }

  /** Table description of table schema_join. Objects of this class serve as prototypes for rows in queries. */
  class SchemaJoin(_tableTag: Tag) extends profile.api.Table[SchemaJoinRow](_tableTag, "schema_join") {
    def * = (fromId, toId, Rep.Some(id)) <> (SchemaJoinRow.tupled, SchemaJoinRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? =
      ((Rep.Some(fromId), Rep.Some(toId), Rep.Some(id))).shaped.<>({ r =>
        import r._; _1.map(_ => SchemaJoinRow.tupled((_1.get, _2.get, _3)))
      }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column from_id SqlType(int4) */
    val fromId: Rep[Int] = column[Int]("from_id")

    /** Database column to_id SqlType(int4) */
    val toId: Rep[Int] = column[Int]("to_id")

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

    /** Foreign key referencing SchemaField (database name data_join_data_field_id_fk) */
    lazy val schemaFieldFk1 = foreignKey("data_join_data_field_id_fk", fromId, SchemaField)(
      r => r.id,
      onUpdate = ForeignKeyAction.NoAction,
      onDelete = ForeignKeyAction.Cascade)

    /** Foreign key referencing SchemaField (database name data_join_data_field_id_fk_2) */
    lazy val schemaFieldFk2 = foreignKey("data_join_data_field_id_fk_2", toId, SchemaField)(
      r => r.id,
      onUpdate = ForeignKeyAction.NoAction,
      onDelete = ForeignKeyAction.Cascade)

    /** Uniqueness Index over (fromId,toId) (database name data_join_pk) */
    val index1 = index("data_join_pk", (fromId, toId), unique = true)
  }

  /** Collection-like TableQuery object for table SchemaJoin */
  lazy val SchemaJoin = new TableQuery(tag => new SchemaJoin(tag))

  /** Entity class storing rows of table SchemaObject
    *  @param name Database column name SqlType(varchar), Length(255,true)
    *  @param schemaSourceId Database column schema_source_id SqlType(int4)
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey */
  case class SchemaObjectRow(name: String, schemaSourceId: Int, id: Option[Int] = None)

  /** GetResult implicit for fetching SchemaObjectRow objects using plain SQL queries */
  implicit def GetResultSchemaObjectRow(implicit e0: GR[String],
                                        e1: GR[Int],
                                        e2: GR[Option[Int]]): GR[SchemaObjectRow] = GR { prs =>
    import prs._
    val r = (<<?[Int], <<[String], <<[Int])
    import r._
    SchemaObjectRow.tupled((_2, _3, _1)) // putting AutoInc last
  }

  /** Table description of table schema_object. Objects of this class serve as prototypes for rows in queries. */
  class SchemaObject(_tableTag: Tag) extends profile.api.Table[SchemaObjectRow](_tableTag, "schema_object") {
    def * = (name, schemaSourceId, Rep.Some(id)) <> (SchemaObjectRow.tupled, SchemaObjectRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? =
      ((Rep.Some(name), Rep.Some(schemaSourceId), Rep.Some(id))).shaped.<>({ r =>
        import r._; _1.map(_ => SchemaObjectRow.tupled((_1.get, _2.get, _3)))
      }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column name SqlType(varchar), Length(255,true) */
    val name: Rep[String] = column[String]("name", O.Length(255, varying = true))

    /** Database column schema_source_id SqlType(int4) */
    val schemaSourceId: Rep[Int] = column[Int]("schema_source_id")

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

    /** Foreign key referencing SchemaSource (database name schema_object_schema_source_id_fk) */
    lazy val schemaSourceFk = foreignKey("schema_object_schema_source_id_fk", schemaSourceId, SchemaSource)(
      r => r.id,
      onUpdate = ForeignKeyAction.NoAction,
      onDelete = ForeignKeyAction.Cascade)
  }

  /** Collection-like TableQuery object for table SchemaObject */
  lazy val SchemaObject = new TableQuery(tag => new SchemaObject(tag))

  /** Entity class storing rows of table SchemaSource
    *  @param name Database column name SqlType(varchar), Length(255,true)
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey */
  case class SchemaSourceRow(name: String, id: Option[Int] = None)

  /** GetResult implicit for fetching SchemaSourceRow objects using plain SQL queries */
  implicit def GetResultSchemaSourceRow(implicit e0: GR[String], e1: GR[Option[Int]]): GR[SchemaSourceRow] = GR { prs =>
    import prs._
    val r = (<<?[Int], <<[String])
    import r._
    SchemaSourceRow.tupled((_2, _1)) // putting AutoInc last
  }

  /** Table description of table schema_source. Objects of this class serve as prototypes for rows in queries. */
  class SchemaSource(_tableTag: Tag) extends profile.api.Table[SchemaSourceRow](_tableTag, "schema_source") {
    def * = (name, Rep.Some(id)) <> (SchemaSourceRow.tupled, SchemaSourceRow.unapply)

    /** Maps whole row to an option. Useful for outer joins. */
    def ? =
      ((Rep.Some(name), Rep.Some(id))).shaped.<>({ r =>
        import r._; _1.map(_ => SchemaSourceRow.tupled((_1.get, _2)))
      }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column name SqlType(varchar), Length(255,true) */
    val name: Rep[String] = column[String]("name", O.Length(255, varying = true))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)

    /** Uniqueness Index over (name) (database name data_source_name_uindex) */
    val index1 = index("data_source_name_uindex", name, unique = true)
  }

  /** Collection-like TableQuery object for table SchemaSource */
  lazy val SchemaSource = new TableQuery(tag => new SchemaSource(tag))
}
