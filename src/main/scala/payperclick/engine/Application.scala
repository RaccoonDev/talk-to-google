package payperclick.engine

import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import org.slf4j.LoggerFactory
import payperclick.engine.domain.{AccountBidsQueueManager, BidRequest, BidSubmissionMetadata, BiddingRequestsManager}

object Application extends App {

  val logger = LoggerFactory.getLogger("ApplicationMain")

  ActorSystem[Nothing](Guardian(), "BiddingEngine")
}

object Guardian {
  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    AccountBidsQueueManager.initSharding(context.system)

    // That's a good place to start HTTP Service

    // Simulating a request here
    val biddingRequestsManager = context.spawn(BiddingRequestsManager(), "biddingRequestsManager")
    biddingRequestsManager ! BidRequest(123456, 22222, 33333, 11.12, BidSubmissionMetadata(UUID.randomUUID(), 100))

    Behaviors.empty
  }
}