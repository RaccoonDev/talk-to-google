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
case class BidRequest(accountId: Long, keywordId: Long, adGroupId: Long, maxCpc: BigDecimal, metadata: BidSubmissionMetadata) extends CborSerializable

object AccountBidsQueueManager {
  def initSharding(system: ActorSystem[_]): Unit = {
    ClusterSharding(system).init(Entity(TypeKey) { entityContext =>
      AccountBidsQueueManager(entityContext.entityId.toLong, PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
    })
  }


  case class QueueDefinition(submissionId: UUID, priority: Int)

  // Command
  sealed trait Command[Reply <: CommandReply] extends CborSerializable {
    def replyTo: ActorRef[Reply]
  }

  final case class UpdateBid(replyTo: ActorRef[OperationResult], request: BidRequest) extends Command[OperationResult]

  //Reply
  sealed trait CommandReply extends CborSerializable
  sealed trait OperationResult extends CommandReply
  case object Acknowledged extends OperationResult
  case class Rejected(reason: String) extends OperationResult

  // Event
  sealed trait Event extends CborSerializable
  case class UpdateBidRequestAdded(request: BidRequest) extends Event

  // State
    case class BidsUpdateQueues(queues: Map[QueueDefinition, Queue[BidRequest]]) extends CborSerializable {
     def applyEvent(event: Event): BidsUpdateQueues = event match {
      case UpdateBidRequestAdded(request) =>
        val k = QueueDefinition(request.metadata.id, request.metadata.priority)
        val nq = queues.getOrElse(k, Queue()) :+ request
        copy(queues = queues + (k -> nq))
    }
  }

  val TypeKey: EntityTypeKey[Command[_]] =
    EntityTypeKey[Command[_]]("BiddingManagement")

  def apply(accountId: Long, persistenceId: PersistenceId): Behavior[Command[_]] = {
    EventSourcedBehavior.withEnforcedReplies(persistenceId, BidsUpdateQueues(Map()), commandHandler(accountId), eventHandler)
  }

  private def commandHandler(accountId: Long): (BidsUpdateQueues, Command[_]) => ReplyEffect[Event, BidsUpdateQueues] = {
    (_, cmd) =>
      cmd match {
        case UpdateBid(replyTo, request) if request.accountId != accountId =>
            Effect.reply(replyTo)(Rejected(s"Account ID of incoming request ${request.accountId} does not match processor account id: ${accountId}"))
        case UpdateBid(replyTo, request) =>
          Effect.persist(UpdateBidRequestAdded(request)).thenReply(replyTo)(_ => Acknowledged)
      }
  }

  private def eventHandler: (BidsUpdateQueues, Event) => BidsUpdateQueues = { (state, event) =>
    state.applyEvent(event)
  }


}


