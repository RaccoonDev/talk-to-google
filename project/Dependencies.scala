import sbt._

object Dependencies {
  lazy val akkaVersion = "2.6.4"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"

  lazy val akkaActor = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  lazy val akkaTestKit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion

  lazy val akkaStreams = "com.typesafe.akka" %% "akka-stream" % akkaVersion

  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val akkaCluster = "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion
  lazy val akkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion

  lazy val akkaPersistence ="com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
  lazy val akkaPersistenceCassandra = "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.0-RC1"

  lazy val akkaPersistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
  lazy val akkaClusterTools = "com.typesafe.akka" % "akka-cluster-tools" % akkaVersion

  lazy val akkaSerializationJackson = "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion

  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http"   % "10.1.11"
  lazy val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.11"

}
