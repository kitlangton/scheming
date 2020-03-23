import sbt._

object Versions {
  lazy val caliban = "0.7.1"
  lazy val zio     = "1.0.0-RC18-2"
  lazy val slick   = "3.3.2"
  lazy val slickPg = "0.18.1"
}

object Dependencies {
  lazy val schemingDependencies = caliban ++ slick

  private lazy val caliban = Seq(
    "com.github.ghostdogpr" %% "caliban"        % Versions.caliban,
    "com.github.ghostdogpr" %% "caliban-http4s" % Versions.caliban,
    "com.github.ghostdogpr" %% "caliban-client" % Versions.caliban
  )
  private lazy val slick = Seq(
    "com.typesafe.slick"  %% "slick"              % Versions.slick,
    "com.typesafe.slick"  %% "slick-codegen"      % Versions.slick,
    "com.typesafe.slick"  %% "slick-hikaricp"     % Versions.slick,
    "org.postgresql"      % "postgresql"          % "42.2.5",
    "com.github.tminglei" %% "slick-pg"           % Versions.slickPg,
    "com.github.tminglei" %% "slick-pg_joda-time" % Versions.slickPg
  )

}
