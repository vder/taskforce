import sbt._

object Dependencies {
  lazy val catsEffVersion = "2.5.0"
  lazy val catsVersion = "2.6.0"
  lazy val doobieVersion = "0.13.0"
  lazy val flywayVersion = "7.8.1"
  lazy val pureConfigVersion = "0.14.0"
  lazy val refinedVersion = "0.9.23"
  lazy val CirceVersion = "0.13.0"
  lazy val Http4sVersion = "0.21.22"
  lazy val LogbackVersion = "1.2.3"
  lazy val jwtAuthVersion = "0.0.6"
  lazy val logback = "ch.qos.logback" % "logback-classic" % LogbackVersion
  lazy val slf4j = "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1"
  lazy val pureConfig =
    "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
  lazy val pureConfigCE =
    "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion
  lazy val pureConfigRefined =
    "eu.timepit" %% "refined-pureconfig" % refinedVersion
  lazy val refined = "eu.timepit" %% "refined" % refinedVersion
  lazy val circe = "io.circe" %% "circe-generic" % CirceVersion
  lazy val circeExtras = "io.circe" %% "circe-generic-extras" % CirceVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % CirceVersion
  lazy val circeDerivation = "io.circe" %% "circe-derivation" % "0.13.0-M4"
  // https://mvnrepository.com/artifact/io.circe/circe-refined
  lazy val circeRefined = "io.circe" %% "circe-refined" % CirceVersion
  lazy val flyway = "org.flywaydb" % "flyway-core" % flywayVersion
  lazy val jwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % jwtAuthVersion
  // https://mvnrepository.com/artifact/com.softwaremill.common/id-generator
  lazy val idGen = "com.softwaremill.common" %% "id-generator" % "1.3.1"

  lazy val http4s = Seq(
    "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-circe" % Http4sVersion,
    "org.http4s" %% "http4s-dsl" % Http4sVersion
  )

  lazy val doobie = "org.tpolecat" %% "doobie-core" % doobieVersion
  lazy val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion
  lazy val doobieHikari = "org.tpolecat" %% "doobie-hikari" % doobieVersion
  lazy val doobieRefined = "org.tpolecat" %% "doobie-refined" % doobieVersion

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % catsEffVersion
}
