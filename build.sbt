ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.smartjava"
ThisBuild / organizationName := "smartjava"

val CatsVersion = "3.1.1"
val Http4sVersion = "1.0.0-M23"

lazy val hello = (project in file("."))
  .settings(
    name := "Exploring Tagless",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % CatsVersion,
      "com.github.pureconfig" %% "pureconfig" % "0.15.0",
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "ch.qos.logback" % "logback-core" % "1.2.3",
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "co.fs2" %% "fs2-core" % "3.0.4",
      "co.fs2" %% "fs2-io" % "3.0.4",
      "co.fs2" %% "fs2-reactive-streams" % "3.0.4",
      // JSON Mapping
      "io.circe" %% "circe-generic" % "0.12.3",
      "io.circe" %% "circe-literal" % "0.12.3",
      "org.mongodb.scala" %% "mongo-scala-driver" % "4.2.3"
    ),
    scalacOptions += "-Ymacro-annotations"
  )

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full)
