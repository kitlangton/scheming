package scheming

import java.util.concurrent.TimeUnit
import scheming.DatabaseDriver.api._
import slick.codegen.SourceCodeGenerator
import slick.sql.SqlProfile.ColumnOption

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object DatabaseCodegen extends App {
  // fetch data model
  implicit val ctx = global
  val url          = "jdbc:postgresql://localhost:5432/experiment"
  val db           = Database.forURL(url, user = "postgres")
  val modelAction  = DatabaseDriver.createModel(Option(DatabaseDriver.defaultTables)) // you can filter specific tables here
  val modelFuture  = db.run(modelAction)
  val codegenFuture =
    modelFuture.map(model =>
      new SourceCodeGenerator(model) {
        override def packageCode(profile: String,
                                 pkg: String,
                                 container: String,
                                 parentType: Option[String]): String = {
          s"""
package ${pkg}
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object ${container} extends {
  val profile = $profile
} with ${container}

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait ${container}${parentType.map(t => s" extends $t").getOrElse("")} {
  val profile: $profile
  import profile.api._
  ${indent(code)}
}
      """.trim()
        }

        override def code: String =
          "import scheming.DatabaseDriver._\n" + super.code
        override def Table = new Table(_) {
          override def autoIncLast: Boolean = true
          // override contained column generator
          override def Column = new Column(_) {
            override def asOption: Boolean = autoInc
            // use the data model member of this column to change the Scala type,
            // e.g. to a custom enum or anything else
            override def rawType = {
              model.options
                .collectFirst {
                  case ColumnOption.SqlType(typeName) => typeName
                }
                .collect {
                  case "pull_request_state"        => "PullRequestState"
                  case "pull_request_review_state" => "PullRequestReviewState"
                }
                .getOrElse(super.rawType)
            }
          }
        }
    })

  codegenFuture.onComplete {
    case Success(codegen) =>
      codegen.writeToFile(
        profile = "scheming.DatabaseDriver",
        folder = "src/main/scala/",
        pkg = "scheming",
      )
    case Failure(err) => println("DatabaseCodegen failed", err.toString)
  }
  Await.result(codegenFuture, Duration(50, TimeUnit.SECONDS))
  Thread.sleep(1000)
}
