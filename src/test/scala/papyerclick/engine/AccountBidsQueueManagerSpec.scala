package papyerclick.engine

import java.util.UUID

import akka.actor.testkit.typed.FishingOutcome
import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.actor.typed.eventstream.EventStream
import akka.persistence.journal.inmem.InmemJournal
import akka.persistence.typed.PersistenceId
import org.scalatest.wordspec.AnyWordSpecLike
import payperclick.engine.domain.{AccountBidsQueueManager, Bid, BidRequest, BidSubmissionMetadata}
import payperclick.engine.domain.AccountBidsQueueManager.{Acknowledged, BatchOperationReply, BatchToSend, GiveNextBatch, OperationResult, Rejected, UpdateBid, UpdateBidRequestAdded}

import scala.concurrent.duration._

class AccountBidsQueueManagerSpec extends ScalaTestWithActorTestKit(
  s"""
     |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
     |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
     |akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
     |""".stripMargin) with AnyWordSpecLike with LogCapturing {

  "AccountBidsQueueManager" must {
    val eventProbe = createTestProbe[InmemJournal.Operation]()
    system.eventStream ! EventStream.Subscribe(eventProbe.ref)

    "handle proper bid request" in {
      val probe = createTestProbe[OperationResult]()
      val ref = spawn(AccountBidsQueueManager(123456789, PersistenceId("AccountBidsQueueManager", "123456789")))
      val request = BidRequest(123456789, 1, 2, 0.30, BidSubmissionMetadata(UUID.randomUUID(), 10))

      ref ! UpdateBid(probe.ref, request)

      probe.expectMessage(Acknowledged)
      eventProbe.expectMessageType[InmemJournal.Write].event should ===(UpdateBidRequestAdded(request))
    }

    "reject new bid request if account id not match" in {
      val probe = createTestProbe[OperationResult]()
      val ref = spawn(AccountBidsQueueManager(123456789, PersistenceId("AccountBidsQueueManager", "123456789")))
      val request = BidRequest(123456788, 1, 2, 0.30, BidSubmissionMetadata(UUID.randomUUID(), 10))

      ref ! UpdateBid(probe.ref, request)

      probe.expectMessage(Rejected("Account ID of incoming request 123456788 does not match processor account id: 123456789"))
      eventProbe.expectNoMessage()
    }

    "respond with a batch of next bids to update when asked" in {
      val batchProcessingProbe = createTestProbe[BatchOperationReply]()
      val ref = spawn(AccountBidsQueueManager(123456789, PersistenceId("AccountBidsQueueManager", "123456789")))

      val updateBidProbe = createTestProbe[OperationResult]()
      ref ! UpdateBid(updateBidProbe.ref, BidRequest(123456789, 1, 2, 0.30, BidSubmissionMetadata(UUID.randomUUID(), 10)))

      val nextBatchRequest = GiveNextBatch(batchProcessingProbe.ref, accountId = 123456789, batchSize = 10)

      ref ! nextBatchRequest

      batchProcessingProbe.fishForMessage(3.seconds) {
        case BatchToSend(123456789, _, List(Bid(123456789, 1, 2, bidValue))) if bidValue == BigDecimal(0.30) => FishingOutcome.Complete
      }
    }
  }

}
