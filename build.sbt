name := "api-integrations"

ThisBuild / organization := "com.quantemplate"
ThisBuild / scalaVersion := "3.0.0"
ThisBuild / version := "0.1.4"

val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.2.4"
val CirceVersion = "0.14.1"

lazy val root = (project in file("."))
  .aggregate(
    integrations
  )

lazy val integrations = (project in file("integrations"))
  .settings(
    // scala 2.13 deps
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.36.0",
      "com.norbitltd" %% "spoiwo" % "1.7.0",
      "com.github.scopt" %% "scopt" % "4.0.1"
    ).map(_.cross(CrossVersion.for3Use2_13)),

    // java deps
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe" % "config" % "1.4.1",
      "com.google.maps" % "google-maps-services" % "0.18.0",
      "org.mockito" % "mockito-core" % "3.9.0" % Test
    ),

    // native scala 3 deps
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.26" % Test,
      "org.typelevel" %% "cats-core" % "2.6.1",
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-yaml" % "0.14.0"
    ),
    assembly / mainClass := Some("com.quantemplate.integrations.Main"),
    assembly / assemblyJarName := s"qt-integrations-${version.value}.jar",
    scalacOptions ++= Seq(
      // options adapted from https://nathankleyn.com/2019/05/13/recommended-scalac-flags-for-2-13/
      // and https://docs.scala-lang.org/scala3/guides/migration/options-lookup.html
      // a lot of `-Xlint:<warnings>` and `-Ywarn-unused:<warnings>` are not yet available for Scala 3
      "-deprecation",
      "-explain-types",
      "-feature",
      "-unchecked",
      "-Ykind-projector:underscores",
      "-Xfatal-warnings",

      // new Scala 3 options: https://docs.scala-lang.org/scala3/guides/migration/options-new.html
      "-explain",
      "-new-syntax"
    )
  )

Compile / run / fork := true
