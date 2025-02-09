import Dependencies.Libraries._
import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / organization                        := "com.pfl"
ThisBuild / organizationName                    := "pfl"
ThisBuild / scalaVersion                        := "2.13.15"
ThisBuild / version                             := "0.1.0-SNAPSHOT"

IntegrationTest / parallelExecution in Global := false

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(FlywayPlugin)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(RevolverPlugin)
  .configs(IntegrationTest.extend(Test))
  .settings(
    name           := "taskforce",
    flywayUrl      := "jdbc:postgresql://localhost:54340/task",
    flywayUser     := "vder",
    flywayPassword := "password",
    Defaults.itSettings,
    libraryDependencies += flywayPostgres,
    publish              := {},
    publish / skip       := true,
    Docker / packageName := "taskforce",
    dockerCommands := dockerCommands.value.flatMap {
      case cmd @ Cmd("FROM", _) => List(cmd, Cmd("RUN", "apk update && apk add bash"))
      case other                => List(other)
    },
    dockerExposedPorts ++= Seq(9090),
    dockerBaseImage    := "openjdk:8-jre-alpine",
    dockerUpdateLatest := true,
    semanticdbEnabled  := true,                        // enable SemanticDB
    //semanticdbVersion  := scalafixSemanticdb.revision, // only required for Scala 2.x
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      "-Xlint:unused",
      "-Ymacro-annotations"
    )
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
      pureConfigGeneric,
      pureConfigRefined,
      scalaCheckEffect,
      scalaCheckEffectMunit,
      simulacrum,
      slf4j,
      tapir,
      tapirCirce,
      tapirCats,
      tapirRefined,
      tapirSwagger,
      tapirHttp4s
    ).map(_.exclude("org.slf4j", "*")),
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
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
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
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
    common         % "test->test;it->it;compile->compile;test->it"
  )

lazy val filters = (project in file("modules/filters"))
  .disablePlugins(RevolverPlugin)
  .configs((IntegrationTest extend Test))
  .settings(
    Defaults.itSettings,
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
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
    circeExtras,
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
    sttp3Circe % Test
  ).map(_.exclude("org.slf4j", "*")),
  addCompilerPlugin(kindProjector),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-language:higherKinds",
    "-language:postfixOps",
    "-feature",
    "-Xlint:unused",
    "-Ymacro-annotations"
  )
)
