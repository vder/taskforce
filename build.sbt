import Dependencies.Libraries._
import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / githubWorkflowPublishTargetBranches := Seq()
ThisBuild / organization                        := "com.pfl"
ThisBuild / organizationName                    := "pfl"
ThisBuild / scalaVersion                        := "2.13.8"
ThisBuild / version                             := "0.1.0-SNAPSHOT"

IntegrationTest / parallelExecution := false

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(FlywayPlugin)
  .enablePlugins(AshScriptPlugin)
  .configs(IntegrationTest.extend(Test))
  .settings(
    name           := "taskforce",
    flywayUrl      := "jdbc:postgresql://localhost:54340/task",
    flywayUser     := "vder",
    flywayPassword := "password",
    Defaults.itSettings,
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
    semanticdbVersion  := scalafixSemanticdb.revision, // only required for Scala 2.x
    libraryDependencies ++= Seq(
      flyway,
      logback,
      pureConfig,
      pureConfigCE,
      pureConfigRefined,
      
    ).map(_.exclude("org.slf4j", "*")),
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
    common % "test->test",
    filters % "compile->compile;test->test"
  )
  .aggregate(
    filters,
    projects,
    tasks
  )

lazy val common = (project in file("common"))
  .settings(
    libraryDependencies ++= Seq(
      cats,
      circe,
      doobie,
      doobieQuill,
      http4sCirce,
      http4sDsl,
      monixNewType,
      monixNewTypeCirce,
      mUnit,
      mUnitCE,
      mUnitScalacheck,
      simulacrum,
      scalaCheckEffect,
      scalaCheckEffectMunit
    ).map(_.exclude("org.slf4j", "*")),
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
  )

lazy val authentication = (project in file("auth"))
  .settings(
    libraryDependencies ++= Seq(
      circeParser,
      doobiePostgres,
      http4sServer,
      jwtCirce
    ).map(_.exclude("org.slf4j", "*")),
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
  )
  .dependsOn(common)

lazy val projects = (project in file("projectsFeature"))
  .settings(
    libraryDependencies ++= Seq(
      circeDerivation,
      circeExtras,
      circeFs2,
      circeParser,
      circeRefined,
      doobieHikari,
      doobiePostgres,
      doobieRefined,
      http4sClient,
      http4sCirce,
      refined,
      refinedCats
    ).map(_.exclude("org.slf4j", "*")),
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
  )
  .dependsOn(
    authentication % "compile->compile;test->test",
    common         % "test->test"
  )

lazy val tasks = (project in file("tasksFeature"))
  .settings(
    libraryDependencies ++= Seq(
      circeDerivation,
      circeExtras,
      circeFs2,
      circeParser,
      circeRefined,
      doobieHikari,
      doobiePostgres,
      doobieRefined,
      http4sClient,
      http4sCirce,
      refined,
      refinedCats
    ).map(_.exclude("org.slf4j", "*")),
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
  )
  .dependsOn(
    authentication % "compile->compile;test->test",
    common         % "test->test"
  )

lazy val filters = (project in file("filtersFeature"))
  .settings(
    libraryDependencies ++= Seq(
      log4cats,
      slf4j
    ).map(_.exclude("org.slf4j", "*")),
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
  )
  .dependsOn(
    tasks    % "compile->compile;test->test",
    projects % "compile->compile;test->test"
  )
