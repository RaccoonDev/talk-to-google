package payperclick.engine

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Application extends App {
  val logger = LoggerFactory.getLogger("ApplicationMain")
  ActorSystem[Nothing](Guardian(0), "BiddingEngine")
}

object AnotherApplicationInstance extends App {
  val logger = LoggerFactory.getLogger("AnotherApplicationMain")
  val config = ConfigFactory.parseString(
    """
       |akka.remote.artery.canonical.port = 2552
       |""".stripMargin
  ).withFallback(ConfigFactory.load())

  ActorSystem[Nothing](Guardian(0), "BiddingEngine", config)
}