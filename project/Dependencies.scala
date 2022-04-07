import sbt._

object Dependencies {
  object V {
    val Logback          = "1.2.11"
    val betterMonadicFor = "0.3.1"
    val cats             = "2.6.0"
    val catsEff          = "3.3.11"
    val circe            = "0.14.0"
    val circeDerivation  = "0.13.0-M5"
    val doobie           = "1.0.0-RC2"
    val doobieQuill      = "0.0.5"
    val jwtCirce         = "9.0.5"
    val flyway           = "8.5.7"
    val http4s           = "0.23.11"
    val kindProjector    = "0.13.2"
    val munit            = "0.7.29"
    val newType          = "0.4.4"
    val pureConfig       = "0.17.1"
    val refined          = "0.9.28"
    val scalacheckEffect = "1.0.3"
    val simulacrum       = "1.0.1"
    val log4cats = "2.2.0"
    val slf4j = "1.7.36"
  }
  object Libraries {

    def circeLib(artifact: String): ModuleID                    = "io.circe"      %% artifact % V.circe
    def doobieLib(artifact: String): ModuleID                   = "org.tpolecat"  %% artifact % V.doobie
    def http4sLib(artifact: String): ModuleID                   = "org.http4s"    %% artifact % V.http4s
    def mUnitLib(artifact: String): ModuleID                    = "org.scalameta" %% artifact % V.munit % "it,test"
    def refinedLib(artifact: String): ModuleID                  = "eu.timepit"    %% artifact % V.refined
    def typeLevelLibTest(artifact: String, v: String): ModuleID = "org.typelevel" %% artifact % v       % "it,test"

    val catsEffect      = "org.typelevel"        %% "cats-effect"      % V.catsEff
    val circe           = circeLib("circe-generic")
    val circeDerivation = "io.circe"             %% "circe-derivation" % V.circeDerivation
    val circeExtras     = circeLib("circe-generic-extras")
    val circeFs2        = circeLib("circe-fs2")
    val circeParser     = circeLib("circe-parser")
    val circeRefined    = circeLib("circe-refined")
    val doobie          = doobieLib("doobie-core")
    val doobieHikari    = doobieLib("doobie-hikari")
    val doobiePostgres  = doobieLib("doobie-postgres")
    val doobieRefined   = doobieLib("doobie-refined")
    val doobieQuill     = "org.polyvariant" %% "doobie-quill" % V.doobieQuill
    val flyway          = "org.flywaydb"          % "flyway-core"      % V.flyway
    val http4sCirce     = http4sLib("http4s-circe")
    val http4sClient    = http4sLib("http4s-blaze-client")
    val http4sDsl       = http4sLib("http4s-dsl")
    val http4sServer    = http4sLib("http4s-blaze-server")
    val jwtCirce        = "com.github.jwt-scala" %% "jwt-circe"        % V.jwtCirce
    val logback         = "ch.qos.logback"        % "logback-classic"  % V.Logback
    val mUnit           = mUnitLib("munit")
    val mUnitCE         = typeLevelLibTest("munit-cats-effect-3", "1.0.7")
    val mUnitScalacheck = mUnitLib("munit-scalacheck")
    val newType = "io.estatico" %% "newtype" % V.newType
    val pureConfig            = "com.github.pureconfig" %% "pureconfig"             % V.pureConfig
    val pureConfigCE          = "com.github.pureconfig" %% "pureconfig-cats-effect" % V.pureConfig
    val pureConfigRefined     = refinedLib("refined-pureconfig")
    val refined               = refinedLib("refined")
    val scalaCheckEffect      = typeLevelLibTest("scalacheck-effect", V.scalacheckEffect)
    val scalaCheckEffectMunit = typeLevelLibTest("scalacheck-effect-munit", V.scalacheckEffect)
    val log4cats              = "org.typelevel"         %% "log4cats-slf4j"         % V.log4cats
    val slf4j                 = "org.slf4j"             %  "slf4j-api" % V.slf4j
    val simulacrum            = "org.typelevel"         %% "simulacrum"             % V.simulacrum

    // Compiler plugins
    val betterMonadicFor = "com.olegpy"   %% "better-monadic-for" % V.betterMonadicFor
    val kindProjector    = "org.typelevel" % "kind-projector"     % V.kindProjector cross CrossVersion.full
  }
}
