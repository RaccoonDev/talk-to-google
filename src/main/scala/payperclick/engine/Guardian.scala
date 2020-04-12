package payperclick.engine

import java.util.UUID

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import payperclick.engine.domain.{AccountBidsQueueManager, BidRequest, BidSubmissionMetadata, BiddingRequestsManager}
import payperclick.engine.http.{EngineHttpServer, EngineRoutes}

object Guardian {
  def apply(httpPort: Int): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    AccountBidsQueueManager.initSharding(context.system)

    // That's a good place to start HTTP Service
    val routes = new EngineRoutes(context.system)
    EngineHttpServer.start(routes.engine, httpPort, context.system)

    // Simulating a request here
    val biddingRequestsManager = context.spawn(BiddingRequestsManager(), "biddingRequestsManager")
    biddingRequestsManager ! BidRequest(223456, 22222, 33333, 11.12, BidSubmissionMetadata(UUID.randomUUID(), 100))

    Behaviors.empty
  }
}
