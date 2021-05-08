import Dependencies.Libraries._

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.pfl"
ThisBuild / organizationName := "pfl"

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  //.enablePlugins(DockerPlugin)
  .enablePlugins(FlywayPlugin)
  .settings(
    name := "taskforce",
    flywayUrl := "jdbc:postgresql://localhost:54340/taskforce_db",
    flywayUser := "vder",
    flywayPassword := "gordon",
    Docker / packageName := "tf-dockerized",
    dockerExposedPorts ++= Seq(9090),
    dockerBaseImage := "openjdk:8-jre-alpine",
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      catsEffect,
      circe,
      circeDerivation,
      circeExtras,
      circeParser,
      circeRefined,
      doobie,
      doobieHikari,
      doobiePostgres,
      doobieRefined,
      flyway,
      http4sCirce,
      http4sDsl,
      http4sClient,
      http4sServer,
      jwtAuth,
      logback,
      mUnit,
      mUnitCE,
      mUnitScalacheck,
      pureConfig,
      pureConfigCE,
      pureConfigRefined,
      refined,
      scalaCheckEffect,
      scalaCheckEffectMunit,
      slf4j,
      testcontainers,
      testcontainersPostgres
    ),
    addCompilerPlugin(kindProjector),
    addCompilerPlugin(betterMonadicFor),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      //  "-Xfatal-warnings",
      "-Xlint:unused"
    )
  )
