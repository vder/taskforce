import Dependencies.Libraries._
import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / githubWorkflowPublishTargetBranches := Seq()
ThisBuild / organization                        := "com.pfl"
ThisBuild / organizationName                    := "pfl"
ThisBuild / scalaVersion                        := "2.13.8"
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
    filters % "compile->compile;test->test",
    stats % "compile->compile;test->test"
  )
  .aggregate(
    filters,
    projects,
    stats,
    tasks
  )

lazy val common = (project in file("common"))
  .disablePlugins(RevolverPlugin)
  .configs(IntegrationTest extend Test)
  .settings(
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      cats,
      circe,
      doobie,
      doobieQuill,
      flyway,
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
      simulacrum,
      scalaCheckEffect,
      scalaCheckEffectMunit,
      slf4j
    ).map(_.exclude("org.slf4j", "*")),
    addCompilerPlugin(kindProjector),

    scalacOptions ++= Seq("-Ymacro-annotations")
  )

lazy val authentication = (project in file("auth"))
  .disablePlugins(RevolverPlugin)
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
  .disablePlugins(RevolverPlugin)
  .configs((IntegrationTest extend Test))
  .settings(
    Defaults.itSettings,
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
    common         % "test->test;it->it;test->it"
  )


lazy val tasks = (project in file("tasksFeature"))
  .disablePlugins(RevolverPlugin)
  .configs((IntegrationTest extend Test))
  .settings(
    Defaults.itSettings,
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
      mUnit,
      mUnitCE,
      mUnitScalacheck,
      refined,
      refinedCats
    ).map(_.exclude("org.slf4j", "*")),
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
  )
  .dependsOn(
    authentication % "compile->compile;test->test",
    common         % "test->test;it->it;compile->compile;test->it"
  )

lazy val filters = (project in file("filtersFeature"))
  .disablePlugins(RevolverPlugin)
  .configs((IntegrationTest extend Test))
  .settings(
    Defaults.itSettings,
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


  lazy val stats = (project in file("statsFeature"))
  .disablePlugins(RevolverPlugin)
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
      refinedCats,
      log4cats
    ).map(_.exclude("org.slf4j", "*")),
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
  )
  .dependsOn(
    authentication % "compile->compile;test->test",
    common         % "test->test"
  )