package scheming
import com.github.tminglei.slickpg._
import com.github.tminglei.slickpg.str.{PgStringExtensions, PgStringSupport}

trait DatabaseDriver
    extends ExPostgresProfile
    with PgEnumSupport
    with PgDateSupportJoda
    with PgStringSupport
    with PgStringExtensions {

  override val api = MyAPI

  object MyAPI extends API with DateTimeImplicits with PgStringImplicits {
    implicit val strListTypeMapper =
      new SimpleArrayJdbcType[String]("text").to(_.toList)
  }
}

object DatabaseDriver extends DatabaseDriver
