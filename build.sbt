import Dependencies._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.devraccoon"
ThisBuild / organizationName := "payperclick"

lazy val akkaVersion = "2.6.3"
lazy val cassandraVersion = "0.103"

lazy val root = (project in file("."))
  .settings(
    name := "ppc-engine",
    libraryDependencies ++= Seq(
//      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
//      "com.typesafe.akka" %% "akka-persistence-cassandra" % cassandraVersion,
//      "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % cassandraVersion % Test,
      scalaTest % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
