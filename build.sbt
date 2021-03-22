name := "data-ingress"

ThisBuild / organization := "com.quantemplate"
ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "1.0"


// lazy val root = (project in file(".")).
//   settings(
//     inThisBuild(List(
//       organization := "ch.epfl.scala",
//       scalaVersion := "2.13.3"
//     )),
//     name := "hello-world"
//   )

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
      "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.35.3",

      // circe
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,

      // misc
      "org.scalatest" %% "scalatest" % "3.1.0" % Test,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.github.pureconfig" %% "pureconfig" % "0.14.1"
    )
  )