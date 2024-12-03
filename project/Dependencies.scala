import sbt._

object Dependencies {
  object V {
    val Logback          = "1.2.11"
    val betterMonadicFor = "0.3.1"
    val cats             = "2.9.0"
    val catsEff          = "3.5.7"
    val circe            = "0.14.0"

    val doobie           = "1.0.0-RC5"
    val doobieQuill      = "4.8.5"
    val jwtCirce         = "9.0.5"
    val flyway           = "11.0.0"
    val http4s           = "0.23.11"
    val kindProjector    = "0.13.3"
    val munit            = "1.0.0"
    val monixNewType     = "0.3.0"
    val pureConfig       = "0.17.8"
    val refined          = "0.11.2"
    val scalacheckEffect = "2.0-66e864e"
    val simulacrum       = "1.0.1"
    val log4cats         = "2.2.0"
    val slf4j            = "1.7.36"
  }
  object Libraries {

    def circeLib(artifact: String): ModuleID                    = "io.circe"      %% artifact % V.circe
    def doobieLib(artifact: String): ModuleID                   = "org.tpolecat"  %% artifact % V.doobie
    def http4sLib(artifact: String): ModuleID                   = "org.http4s"    %% artifact % V.http4s
    def mUnitLib(artifact: String): ModuleID                    = "org.scalameta" %% artifact % V.munit
    def refinedLib(artifact: String): ModuleID                  = "eu.timepit"    %% artifact % V.refined
    def typeLevelLibTest(artifact: String, v: String): ModuleID = "org.typelevel" %% artifact % v

    val cats                  = "org.typelevel"         %% "cats-core" % V.cats
    val catsEffect      = "org.typelevel"        %% "cats-effect"      % V.catsEff
    val circe           = circeLib("circe-generic")
    val circeExtras     = circeLib("circe-generic-extras")
    val circeFs2        = circeLib("circe-fs2")
    val circeParser     = circeLib("circe-parser")
    val circeRefined    = circeLib("circe-refined")
    val doobie          = doobieLib("doobie-core")
    val doobieHikari    = doobieLib("doobie-hikari")
    val doobiePostgres  = doobieLib("doobie-postgres")
    val doobieRefined   = doobieLib("doobie-refined")
    val doobieQuill     = "io.getquill" %% "quill-doobie" % V.doobieQuill
    val flyway          = "org.flywaydb"          % "flyway-core"      % V.flyway
    val flywayPostgres = "org.flywaydb" % "flyway-database-postgresql" % V.flyway % "runtime"
    val http4sCirce     = http4sLib("http4s-circe")
    val http4sClient    = http4sLib("http4s-blaze-client")
    val http4sDsl       = http4sLib("http4s-dsl")
    val http4sServer    = http4sLib("http4s-blaze-server")
    val jwtCirce        = "com.github.jwt-scala" %% "jwt-circe"        % V.jwtCirce
    val logback         = "ch.qos.logback"        % "logback-classic"  % V.Logback
    val mUnit           = mUnitLib("munit")
    val mUnitCE         = typeLevelLibTest("munit-cats-effect", "2.0.0") % Test
    val mUnitScalacheck = mUnitLib("munit-scalacheck")
    val monixNewType          =          "io.monix" %% "newtypes-core" %   V.monixNewType
    val monixNewTypeCirce     =          "io.monix" %% "newtypes-circe-v0-14" %   V.monixNewType
    val pureConfig            = "com.github.pureconfig" %% "pureconfig-core"             % V.pureConfig
    val pureConfigGeneric            = "com.github.pureconfig" %% "pureconfig-generic"             % V.pureConfig
    val pureConfigCE          = "com.github.pureconfig" %% "pureconfig-cats-effect" % V.pureConfig
    val pureConfigRefined     = refinedLib("refined-pureconfig")
    val refined               = refinedLib("refined")
    val refinedCats           = refinedLib("refined-cats")
    val scalaCheckEffect      = typeLevelLibTest("scalacheck-effect", V.scalacheckEffect)
    val scalaCheckEffectMunit = typeLevelLibTest("scalacheck-effect-munit", V.scalacheckEffect)
    val log4cats              = "org.typelevel"         %% "log4cats-slf4j"         % V.log4cats
    val slf4j                 = "org.slf4j"              % "slf4j-api"              % V.slf4j
    val simulacrum            = "org.typelevel"         %% "simulacrum"             % V.simulacrum

    // Compiler plugins
    val betterMonadicFor = "com.olegpy"   %% "better-monadic-for" % V.betterMonadicFor
    val kindProjector    = "org.typelevel" % "kind-projector"     % V.kindProjector cross CrossVersion.full
  }
}
