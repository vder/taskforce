import sbt._

object Dependencies {
  object V {
    val Logback          = "1.2.6"
    val betterMonadicFor = "0.3.1"
    val cats             = "2.6.0"
    val catsEff          = "3.2.9"
    val circe            = "0.14.0"
    val circeDerivation  = "0.13.0-M5"
    val doobie           = "1.0.0-M5"
    val http4s           = "0.23.6"
    val flyway           = "7.8.2"
    val http4s           = "0.23.6"
    val jwtCirce         = "9.0.2"
    val kindProjector    = "0.13.0"
    val munit            = "0.7.29"
    val pureConfig       = "0.17.0"
    val refined          = "0.9.23"
    val scalacheckEffect = "1.0.3"
    val simulacrum       = "1.0.1"
  }
  object Libraries {

    def circeLib(artifact: String): ModuleID                    = "io.circe"      %% artifact % V.circe
    def doobieLib(artifact: String): ModuleID                   = "org.tpolecat"  %% artifact % V.doobie
    def http4sLib(artifact: String): ModuleID                   = "org.http4s"    %% artifact % V.http4s
    def mUnitLib(artifact: String): ModuleID                    = "org.scalameta" %% artifact % V.munit % "it,test"
    def refinedLib(artifact: String): ModuleID                  = "eu.timepit"    %% artifact % V.refined
    def typeLevelLibTest(artifact: String, v: String): ModuleID = "org.typelevel" %% artifact % v       % "it,test"

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
    val jwtCirce              = "com.github.jwt-scala"  %% "jwt-circe"              % V.jwtCirce
    val logback               = "ch.qos.logback"         % "logback-classic"        % V.Logback
    val mUnit                 = mUnitLib("munit")
    val mUnitCE               = typeLevelLibTest("munit-cats-effect-3", "1.0.6")
    val mUnitScalacheck       = mUnitLib("munit-scalacheck")
    val pureConfig            = "com.github.pureconfig" %% "pureconfig"             % V.pureConfig
    val pureConfigCE          = "com.github.pureconfig" %% "pureconfig-cats-effect" % V.pureConfig
    val pureConfigRefined     = refinedLib("refined-pureconfig")
    val refined               = refinedLib("refined")
    val scalaCheckEffect      = typeLevelLibTest("scalacheck-effect", V.scalacheckEffect)
    val scalaCheckEffectMunit = typeLevelLibTest("scalacheck-effect-munit", V.scalacheckEffect)
    val slf4j                 = "org.typelevel"         %% "log4cats-slf4j"         % "2.1.1"
    val simulacrum            = "org.typelevel"         %% "simulacrum"             % V.simulacrum

    // Compiler plugins
    val betterMonadicFor = "com.olegpy"   %% "better-monadic-for" % V.betterMonadicFor
    val kindProjector    = "org.typelevel" % "kind-projector"     % V.kindProjector cross CrossVersion.full

  }
}
