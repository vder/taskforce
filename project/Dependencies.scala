import sbt._

object Dependencies {
  object V {
    val catsEff          = "2.5.0"
    val cats             = "2.6.0"
    val doobie           = "0.13.0"
    val flyway           = "7.8.1"
    val pureConfig       = "0.14.0"
    val refined          = "0.9.23"
    val circe            = "0.13.0"
    val http4s           = "0.21.22"
    val Logback          = "1.2.3"
    val jwtAuth          = "0.0.6"
    val circeDerivation  = "0.13.0-M4"
    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.11.3"
    val munit            = "0.7.22"
    val scalacheckEffect = "0.6.0"
    val testcontainers   = "0.39.3"
  }
  object Libraries {

    def http4sLib(artifact: String): ModuleID                   = "org.http4s"    %% artifact % V.http4s
    def refinedLib(artifact: String): ModuleID                  = "eu.timepit"    %% artifact % V.refined
    def circeLib(artifact: String): ModuleID                    = "io.circe"      %% artifact % V.circe
    def doobieLib(artifact: String): ModuleID                   = "org.tpolecat"  %% artifact % V.doobie
    def mUnitLib(artifact: String): ModuleID                    = "org.scalameta" %% artifact % V.munit          % Test
    def typeLevelLibTest(artifact: String, v: String): ModuleID = "org.typelevel" %% artifact % v                % Test
    def testContainersLibTest(artifact: String): ModuleID       = "com.dimafeng"  %% artifact % V.testcontainers % Test

    val catsEffect            = "org.typelevel"         %% "cats-effect"            % V.catsEff
    val circe                 = circeLib("circe-generic")
    val circeDerivation       = "io.circe"              %% "circe-derivation"       % V.circeDerivation
    val circeExtras           = circeLib("circe-generic-extras")
    val circeFs2              = circeLib("circe-fs2")
    val circeParser           = circeLib("circe-parser")
    val circeRefined          = circeLib("circe-refined")
    val doobie                = doobieLib("doobie-core")
    val doobieHikari          = doobieLib("doobie-hikari")
    val doobiePostgres        = doobieLib("doobie-postgres")
    val doobieRefined         = doobieLib("doobie-refined")
    val flyway                = "org.flywaydb"           % "flyway-core"            % V.flyway
    val http4sCirce           = http4sLib("http4s-circe")
    val http4sClient          = http4sLib("http4s-blaze-client")
    val http4sDsl             = http4sLib("http4s-dsl")
    val http4sServer          = http4sLib("http4s-blaze-server")
    val jwtAuth               = "dev.profunktor"        %% "http4s-jwt-auth"        % V.jwtAuth
    val logback               = "ch.qos.logback"         % "logback-classic"        % V.Logback
    val pureConfig            = "com.github.pureconfig" %% "pureconfig"             % V.pureConfig
    val pureConfigCE          = "com.github.pureconfig" %% "pureconfig-cats-effect" % V.pureConfig
    val pureConfigRefined     = refinedLib("refined-pureconfig")
    val refined               = refinedLib("refined")
    val slf4j                 = "io.chrisdavenport"     %% "log4cats-slf4j"         % "1.1.1"
    val mUnitCE               = typeLevelLibTest("munit-cats-effect-2", "1.0.0")
    val scalaCheckEffect      = typeLevelLibTest("scalacheck-effect", V.scalacheckEffect)
    val scalaCheckEffectMunit = typeLevelLibTest("scalacheck-effect-munit", V.scalacheckEffect)

    // Compiler plugins
    val betterMonadicFor = "com.olegpy"   %% "better-monadic-for" % V.betterMonadicFor
    val kindProjector    = "org.typelevel" % "kind-projector"     % V.kindProjector cross CrossVersion.full

    val mUnit                  = mUnitLib("munit")
    val mUnitScalacheck        = mUnitLib("munit-scalacheck")
    val testcontainers         = testContainersLibTest("testcontainers-scala-munit")
    val testcontainersPostgres = testContainersLibTest("testcontainers-scala-postgresql")
  }
}
