package payperclick.engine

import akka.actor.typed.ActorSystem
import org.slf4j.LoggerFactory

object Application extends App {
  val logger = LoggerFactory.getLogger("ApplicationMain")
  ActorSystem[Nothing](Guardian(), "BiddingEngine")
}
