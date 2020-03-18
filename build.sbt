import Dependencies._

ThisBuild / scalaVersion := "2.12.7"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.devraccoon"
ThisBuild / organizationName := "payperclick"

lazy val akkaVersion = "2.5.13"
lazy val cassandraVersion = "0.103"

resolvers += Resolver.bintrayRepo("akka", "snapshots")
resolvers += Resolver.jcenterRepo

lazy val root = (project in file("."))
  .settings(
    name := "ppc-engine",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-cassandra" % cassandraVersion,
      "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % cassandraVersion % Test,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      scalaTest % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
