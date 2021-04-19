name := "data-ingress"

ThisBuild / organization := "com.quantemplate"
ThisBuild / scalaVersion := "3.0.0-RC1"
ThisBuild / version := "1.0"

val AkkaVersion = "2.6.12"
val AkkaHttpVersion = "10.2.4"
val CirceVersion = "0.14.0-M3"

lazy val root = (project in file("."))
  .aggregate(
    capitaliq
  )

lazy val capitaliq = (project in file("capitaliq"))
  .settings(
    name := "capitaliq",
    libraryDependencies ++= Seq(
      // akka
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.35.3",
      // circe
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.circe" %% "circe-yaml" % "0.13.1",
      // misc
      "org.scalatest" %% "scalatest" % "3.1.0" % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe" % "config" % "1.4.1",
      "org.typelevel" %% "cats-core" % "2.4.2",
      "com.norbitltd" %% "spoiwo" % "1.7.0",
      "com.github.scopt" %% "scopt" % "4.0.1",
    ).map(_.withDottyCompat(scalaVersion.value)),
    assembly / mainClass := Some("com.quantemplate.capitaliq.Main")
  )

Compile / run / fork := true
