package scheming
import zio._
import scheming.SchemableDerivation._

object JiraSource extends App {
  @Source("Jira")
  case class Issue(id: Int, key: String, summary: String, description: String)

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    createSchema[Issue]
      .fold(e => 1, r => 0)
  }
}

object GitHubSource extends App {
  @Source("GitHub")
  case class PullRequest(id: String, title: String, number: Int, pullRequestReviews: HasMany[PullRequestReview])

  @Source("GitHub")
  case class User(id: Int, login: String, name: String, age: Int)

  @Source("GitHub")
  case class PullRequestReview(id: String, author: HasOne[User], state: String)

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    createSchema[PullRequest].fold(e => 1, r => 0)
  }
}

object JiraInput extends App {
  @Source("Jira")
  case class Issue(id: Int, key: String, summary: String)

  val exampleIssue: Issue  = Issue(1, "TRP-123", "Delete all the code")
  val exampleIssue2: Issue = Issue(2, "TRP-124", "Readd all the code")

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    (createSchema[Issue] *>
      insertRecords(exampleIssue, exampleIssue2))
      .fold(e => 1, r => 0)
  }
}

object GitHubInput extends App {
  @Source("GitHub")
  case class PullRequest(id: String, title: String, number: Int, pullRequestReviews: HasMany[PullRequestReview])

  @Source("GitHub")
  case class User(id: String, login: String, name: String, alive: Boolean = true)

  @Source("GitHub")
  case class PullRequestReview(id: String, author: HasOne[User], state: String)

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    val nancy       = User("TSACC123", "nancy", "Nancy Shmancy")
    val kit         = User("OSENTUH8172", "kitlangton", "Kit Langton")
    val kitReview   = PullRequestReview("STNEHU212", HasOne(kit), "approved")
    val nancyReview = PullRequestReview("OSETUH38", HasOne(nancy), "commented")
    val pullRequest = PullRequest(id = "128SOAEUTNH",
                                  title = "Important PR",
                                  number = 23,
                                  pullRequestReviews = HasMany(List(kitReview, nancyReview)))

    (createSchema[PullRequest] *>
      insertRecords(pullRequest)).fold(e => 1, r => 0)
  }
}
