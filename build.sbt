import Dependencies.Libraries._
import com.typesafe.sbt.packager.docker.Cmd

ThisBuild / githubWorkflowPublishTargetBranches := Seq()
ThisBuild / organization := "com.pfl"
ThisBuild / organizationName := "pfl"
ThisBuild / scalaVersion := "2.13.8"
//ThisBuild / scalacOptions += "-P:semanticdb:synthetics:on"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / versionScheme := Some("early-semver")

IntegrationTest / parallelExecution := false

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .enablePlugins(FlywayPlugin)
  .enablePlugins(AshScriptPlugin)
  .configs(IntegrationTest.extend(Test))
  .settings(
    name := "taskforce",
    flywayUrl := "jdbc:postgresql://localhost:54340/task",
    flywayUser := "vder",
    flywayPassword := "password",
    Defaults.itSettings,
    publish := {},
    publish / skip := true,
    Docker / packageName := "taskforce",
    dockerCommands := dockerCommands.value.flatMap {
      case cmd @ Cmd("FROM", _) => List(cmd, Cmd("RUN", "apk update && apk add bash"))
      case other                => List(other)
    },
    dockerExposedPorts ++= Seq(9090),
    dockerBaseImage := "openjdk:8-jre-alpine",
    dockerUpdateLatest := true,
    semanticdbEnabled := true,                        // enable SemanticDB
    semanticdbVersion := scalafixSemanticdb.revision, // only required for Scala 2.x
    libraryDependencies ++= Seq(
    //  catsEffect,
      circe,
      circeDerivation,
      circeExtras,
      circeFs2,
      circeParser,
      circeRefined,
   //   doobie,
      doobieHikari,
      doobiePostgres,
      doobieRefined,
      doobieQuill,
      flyway,
   //   http4sCirce,
      http4sClient,
    //  http4sDsl,
      http4sServer,
      jwtCirce,
      logback,
      mUnit,
      mUnitCE,
      mUnitScalacheck,
      pureConfig,
      pureConfigCE,
      pureConfigRefined,
      refined,
     // quill,
      scalaCheckEffect,
      scalaCheckEffectMunit,
    //  simulacrum,
      slf4j,
      log4cats
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
  .dependsOn(authentication)

lazy val common = (project in file("common")).settings(
  libraryDependencies ++= Seq(cats, http4sDsl, circe, http4sCirce, doobie, simulacrum),
  addCompilerPlugin(kindProjector),
  scalacOptions ++= Seq("-Ymacro-annotations")
)

lazy val authentication = (project in file("auth"))
  .settings(
    libraryDependencies ++= Seq(circeParser, http4sServer, jwtCirce,doobiePostgres),
    addCompilerPlugin(kindProjector),
    scalacOptions ++= Seq("-Ymacro-annotations")
  )
  .dependsOn(common)
