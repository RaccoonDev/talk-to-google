package payperclick.engine.domain

import java.util.UUID

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import payperclick.engine.CborSerializable

import scala.collection.immutable.Queue

// Each bid request is going to be routed by accountId and grouped into batches
// so, each bidRequest should contain its submissionId and priority (the higher number, the higher priority)
case class BidSubmissionMetadata(id: UUID, priority: Int) extends CborSerializable
case class Bid(accountId: Long, keywordId: Long, adGroupId: Long, maxCpc: BigDecimal) extends CborSerializable
case class BidRequest(bid: Bid, metadata: BidSubmissionMetadata) extends CborSerializable {
  val accountId: Long = bid.accountId
}

object BidRequest {
  def apply(accountId: Long, keywordId: Long, adGroupId: Long, maxCpc: BigDecimal, metadata: BidSubmissionMetadata): BidRequest =
    new BidRequest(Bid(accountId, keywordId, adGroupId, maxCpc), metadata)
}

object AccountBidsQueueManager {
  def initSharding(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
      AccountBidsQueueManager(entityContext.entityId.toLong, PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
    })
  }

  // Command
  sealed trait Command[Reply <: CommandReply] extends CborSerializable {
    def replyTo: ActorRef[Reply]
  }

  final case class UpdateBid(replyTo: ActorRef[OperationResult], request: BidRequest) extends Command[OperationResult]
  final case class GiveNextBatch(replyTo: ActorRef[BatchOperationReply], accountId: Long, batchSize: Int) extends Command[BatchOperationReply]

  // Reply for bid update
  sealed trait CommandReply extends CborSerializable
  sealed trait OperationResult extends CommandReply
  case object Acknowledged extends OperationResult
  case class Rejected(reason: String) extends OperationResult

  // Reply for batch request
  sealed trait BatchOperationReply extends CommandReply
  case class BatchToSend(accountId: Long, batchId: UUID, bids: List[Bid]) extends BatchOperationReply

  // Event
  sealed trait Event extends CborSerializable
  case class UpdateBidRequestAdded(request: BidRequest) extends Event
  case class BatchSent(sentItems: List[Bid]) extends Event

  // State
    case class BidsUpdateQueues(bids: Set[Bid]) extends CborSerializable {
     def applyEvent(event: Event): BidsUpdateQueues = event match {
      case UpdateBidRequestAdded(request) =>
        copy(bids = bids + request.bid)
      case BatchSent(items) =>
         copy(bids = bids -- items)
    }
  }

  val TypeKey: EntityTypeKey[Command[_]] =
    EntityTypeKey[Command[_]]("BiddingManagement")

  def apply(accountId: Long, persistenceId: PersistenceId): Behavior[Command[_]] = {
    EventSourcedBehavior.withEnforcedReplies(persistenceId, BidsUpdateQueues(Set()), commandHandler(accountId), eventHandler)
  }

  private def commandHandler(accountId: Long): (BidsUpdateQueues, Command[_]) => ReplyEffect[Event, BidsUpdateQueues] = {
    (state, cmd) =>
      cmd match {
        case UpdateBid(replyTo, request) if request.accountId != accountId =>
            Effect.reply(replyTo)(Rejected(s"Account ID of incoming request ${request.accountId} does not match processor account id: $accountId"))
        case UpdateBid(replyTo, request) =>
          Effect.persist(UpdateBidRequestAdded(request)).thenReply(replyTo)(_ => Acknowledged)
        case GiveNextBatch(replyTo, accountId, batchSize) =>
          val batchToSend = state.bids.take(batchSize).toList
          Effect.persist(BatchSent(batchToSend)).thenReply(replyTo)(_ => BatchToSend(accountId, UUID.randomUUID(), batchToSend))
      }
  }

  private def eventHandler: (BidsUpdateQueues, Event) => BidsUpdateQueues = { (state, event) =>
    state.applyEvent(event)
  }


}


