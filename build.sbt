import Dependencies._

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
    flywayUrl := "jdbc:postgresql://localhost:54340/exchange",
    flywayUser := "vder",
    flywayPassword := "gordon",
    Docker / packageName := "tf-dockerized",
    dockerExposedPorts ++= Seq(9090),
    dockerBaseImage := "openjdk:8-jre-alpine",
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      logback,
      pureConfig,
      refined,
      circe,
      circeExtras,
      circeDerivation,
      flyway,
      doobie,
      doobiePostgres,
      catsEffect,
      slf4j,
      jwtAuth,
      circeParser,
      doobieHikari
    ),
    libraryDependencies ++= http4s,
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature"
      //  "-Xfatal-warnings",
      // "-Xlint:unused"
    )
  )
