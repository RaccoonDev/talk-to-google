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

class AccountBidsQueueManagerSpec
  extends ScalaTestWithActorTestKit(
  s"""
     |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
     |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
     |akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
     |""".stripMargin)
    with AnyWordSpecLike
    with LogCapturing {

  val eventProbe = createTestProbe[InmemJournal.Operation]()
  system.eventStream ! EventStream.Subscribe(eventProbe.ref)

  "AccountBidsQueueManager" when {

    "empty" should {

      "accept proper bid request" in {

        val probe = createTestProbe[OperationResult]()
        val ref = spawn(AccountBidsQueueManager(123456789, PersistenceId("AccountBidsQueueManager", "123456789")))
        val request = BidRequest(123456789, 1, 2, 0.30, BidSubmissionMetadata(UUID.fromString("11111111-1111-1111-1111-111111111111"), 10))

        ref ! UpdateBid(probe.ref, request)

        probe.expectMessage(Acknowledged)

        eventProbe.fishForMessage(3.seconds) {
          case InmemJournal.Write(UpdateBidRequestAdded(`request`), _, _) => FishingOutcome.Complete
          case _ => FishingOutcome.Continue
        }
      }

      "reject new bid request if account id not match" in {
        val eventProbe = createTestProbe[InmemJournal.Operation]()
        system.eventStream ! EventStream.Subscribe(eventProbe.ref)

        val probe = createTestProbe[OperationResult]()
        val ref = spawn(AccountBidsQueueManager(123456789, PersistenceId("AccountBidsQueueManager", "123456789")))
        val request = BidRequest(123456788, 1, 2, 0.30, BidSubmissionMetadata(UUID.fromString("22222222-2222-2222-2222-222222222222"), 10))

        ref ! UpdateBid(probe.ref, request)

        probe.expectMessage(Rejected("Account ID of incoming request 123456788 does not match processor account id: 123456789"))
      }
    }

    "has some bids already in queue" should {
      val batchProcessingProbe = createTestProbe[BatchOperationReply]()
      val accountId: Long = 223456789

      val ref = spawn(AccountBidsQueueManager(accountId, PersistenceId("AccountBidsQueueManager", accountId.toString)))

      val updateBidProbe = createTestProbe[OperationResult]()
      val submissionId = UUID.fromString("33333333-3333-3333-3333-3333333333333333")
      val bids = (1 to 15).map(i => Bid(accountId, 1, i, 0.30)).toList

      bids.foreach { b => ref ! UpdateBid(updateBidProbe.ref, BidRequest(b, BidSubmissionMetadata(submissionId, 10))) }

      "respond with a batch of next bids to update when asked" in {
        val nextBatchRequest = GiveNextBatch(batchProcessingProbe.ref, accountId = accountId, batchSize = 10)

        // Asking for two batches:
        ref ! nextBatchRequest
        ref ! nextBatchRequest

        // Expecting to get one batch of 10 items
        batchProcessingProbe.fishForMessage(3.seconds) {
          case BatchToSend(`accountId`, _, bidsList) if bidsList.size == 10 => FishingOutcome.Complete
          case _ => FishingOutcome.Continue
        }

        // Expecting to get another batch of 5 items
        batchProcessingProbe.fishForMessage(3.seconds) {
          case BatchToSend(`accountId`, _, bidsList) if bidsList.size == 5 => FishingOutcome.Complete
          case _ => FishingOutcome.Continue
        }
      }

    }

  }

}
