import Dependencies.Libraries._
import com.typesafe.sbt.packager.docker.Cmd
import org.typelevel.scalacoptions.ScalacOptions

ThisBuild / organization     := "com.pfl"
ThisBuild / organizationName := "pfl"
ThisBuild / scalaVersion     := "3.6.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild/  tpolecatExcludeOptions ++=  Set(
  ScalacOptions.advancedKindProjector,
  ScalacOptions.privateKindProjector
)

//TODO: migrate IntegrationTest
IntegrationTest / parallelExecution in Global := false

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(FlywayPlugin)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(RevolverPlugin)
  .configs(IntegrationTest.extend(Test))
  .settings(
    name                 := "taskforce",
    flywayUrl            := "jdbc:postgresql://localhost:54340/task",
    flywayUser           := "vder",
    flywayPassword       := "password",
    Defaults.itSettings,
    libraryDependencies += flywayPostgres,
    publish              := {},
    publish / skip       := true,
    Docker / packageName := "taskforce",
    dockerCommands       := dockerCommands.value.flatMap {
      case cmd @ Cmd("FROM", _) => List(cmd, Cmd("RUN", "apk update && apk add bash"))
      case other                => List(other)
    },
    dockerExposedPorts ++= Seq(9090),
    dockerBaseImage      := "openjdk:8-jre-alpine",
    dockerUpdateLatest   := true,
    semanticdbEnabled    := true, // enable SemanticDB
  )
  .dependsOn(
    common  % "test->test",
    filters % "compile->compile;test->test",
    stats   % "compile->compile;test->test"
  )
  .aggregate(
    filters,
    projects,
    stats,
    tasks
  )

lazy val common = (project in file("modules/common"))
  .disablePlugins(RevolverPlugin)
  .configs(IntegrationTest extend Test)
  .settings(
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      cats,
      circe,
      doobieQuill,
      quillCodeGen,
      flyway,
      flywayPostgres,
      http4sCirce,
      http4sDsl,
      log4cats,
      logback,
      monixNewType,
      monixNewTypeCirce,
      mUnit,
      mUnitCE,
      mUnitScalacheck,
      pureConfig,
      pureConfigCE,
      pureConfigRefined,
      scalaCheckEffect,
      scalaCheckEffectMunit,
      slf4j,
      tapir,
      tapirCirce,
      tapirCats,
      tapirRefined,
      tapirSwagger,
      tapirHttp4s
    ).map(_.exclude("org.slf4j", "*")),
  )

lazy val authentication = (project in file("modules/auth"))
  .disablePlugins(RevolverPlugin)
  .settings(
    libraryDependencies ++= Seq(
      circeParser,
      doobiePostgres,
      http4sServer,
      jwtCirce,
      tapir,
      tapirHttp4s,
      tapirCirce,
      tapirCats,
      tapirRefined
    ).map(_.exclude("org.slf4j", "*")),
  )
  .dependsOn(common)

lazy val projects = (project in file("modules/projects"))
  .disablePlugins(RevolverPlugin)
  .configs(IntegrationTest extend Test)
  .settings(Defaults.itSettings, sharedSettings)
  .dependsOn(
    authentication % "compile->compile;test->test",
    common         % "test->test;it->it;test->it"
  )

lazy val tasks = (project in file("modules/tasks"))
  .disablePlugins(RevolverPlugin)
  .configs((IntegrationTest extend Test))
  .settings(Defaults.itSettings, sharedSettings)
  .dependsOn(
    authentication % "compile->compile;test->test",
    common         % "test->test;it->itA;compile->compile;test->it"
  )

lazy val filters = (project in file("modules/filters"))
  .disablePlugins(RevolverPlugin)
  .configs((IntegrationTest extend Test))
  .settings(
    Defaults.itSettings,
    scalacOptions ++= Seq("-Xkind-projector:underscores")
  )
  .dependsOn(
    tasks    % "compile->compile;test->test",
    projects % "compile->compile;test->test"
  )

lazy val stats = (project in file("modules/stats"))
  .disablePlugins(RevolverPlugin)
  .settings(Defaults.itSettings, sharedSettings)
  .dependsOn(
    authentication % "compile->compile;test->test",
    common         % "test->test"
  )

lazy val sharedSettings = Seq(
  libraryDependencies ++= Seq(
    circeFs2,
    circeParser,
    circeRefined,
    doobieHikari,
    doobiePostgres,
    doobieRefined,
    http4sClient,
    refined,
    refinedCats,
    tapir,
    tapirHttp4s,
    tapirCirce,
    sttp3Client % Test,
    tapirServer % Test,
    sttp3Circe  % Test
  ).map(_.exclude("org.slf4j", "*")),
)
