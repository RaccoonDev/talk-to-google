import sbt._

object Dependencies {
  lazy val akkaVersion = "2.6.4"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.8"

  lazy val akkaActor = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  lazy val akkaStreams = "com.typesafe.akka" %% "akka-stream" % akkaVersion

  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"


}
