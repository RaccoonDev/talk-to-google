import Dependencies._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.devraccoon"
ThisBuild / organizationName := "payperclick"

//lazy val cassandraVersion = "0.103"

resolvers += Resolver.bintrayRepo("akka", "snapshots")
resolvers += Resolver.jcenterRepo

lazy val root = (project in file("."))
  .settings(
    name := "ppc-engine",
    libraryDependencies ++= Seq(
      akkaActor,
      logback,
      akkaStreams,
      akkaStreamTyped,
      akkaCluster,
      akkaClusterSharding,
      akkaPersistence,
      akkaPersistenceQuery,
      akkaPersistenceCassandra,
      akkaSerializationJackson,
      akkaHttp,
      akkaHttpSprayJson,
      scalaTest % Test,
      akkaTestKit % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
