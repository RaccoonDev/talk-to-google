package payperclick.engine.domain

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import payperclick.engine.domain.AccountBidsQueueManager.{OperationResult, UpdateBid}

object BiddingRequestsManager {

  def apply(): Behavior[BidRequest] = Behaviors.setup { context =>
    val sharding = ClusterSharding(context.system)
    val ignoringActor: ActorRef[OperationResult] = context.spawn(
      Behaviors.logMessages(Behaviors.ignore[OperationResult]), "ignore")
    apply(sharding, Map(), ignoringActor)
  }

  def apply(sharding: ClusterSharding,
            processors: Map[Long, EntityRef[AccountBidsQueueManager.Command[_]]],
            ignoringBehavior: ActorRef[OperationResult]): Behavior[BidRequest] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        bidRequest: BidRequest =>
          context.log.info("Received bid request")
          processors.get(bidRequest.accountId) match {
            case Some(processor) =>
              processor ! UpdateBid(ignoringBehavior, bidRequest)
              Behaviors.same
            case None =>
              // spawn new processor
              val accountBidsQueueManager =
                sharding.entityRefFor(AccountBidsQueueManager.TypeKey, bidRequest.accountId.toString)
              // send message to the new processor
              accountBidsQueueManager ! UpdateBid(ignoringBehavior, bidRequest)
              apply(sharding, processors + (bidRequest.accountId -> accountBidsQueueManager), ignoringBehavior)
          }
      }
    }
}
