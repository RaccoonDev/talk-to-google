package payperclick.engine

import java.util.UUID

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.slf4j.LoggerFactory
import payperclick.engine.streams.{BiddingRequest, BiddingRequestsFlow, BiddingResults, GoogleAdsApi}

import scala.concurrent.duration._
import scala.util.Random

/**
 * what was I?
 * - Dispatching actor that received submission request
 * - The actor then can create submission controller actor that will deal
 * with the further processing. Receiving and controlling state of things.
 * - assuming that I'm creating an application scoped for one particular account
 */

sealed trait BiddingSubmission {
  def submissionId: String
}

final case class BigQuerySubmission(override val submissionId: String) extends BiddingSubmission

object SubmissionManager {
  def apply(): Behavior[BiddingSubmission] =
    Behaviors.receive( (context, message) => message match {
      case BigQuerySubmission(submissionId) =>
        println(s"Received submission request with submission id: $submissionId")

        implicit val materializer: Materializer = Materializer(context)

        val random = new Random()
        // Here I should start stream that reads from specified big query table
        // and writes to Google Ads API
        Source(1 to 126984)
          .map(_ => BiddingRequest(accountId = 1, random.nextLong(), random.nextLong(), maxCpc = random.nextDouble()))
          .groupedWithin(10000, 1.minute)
          .via(Flow.fromGraph(new BiddingRequestsFlow((req: Seq[BiddingRequest]) => {
            Thread.sleep(random.between(50, 350))
            req.map(BiddingResults(_, random.nextBoolean(), random.nextString(20)))
          })))
          .runWith(Sink.foreach[Seq[BiddingResults]](r => println(s"Received results if size: ${r.size}")))


        Behaviors.same
    })
}

object BiddingPipeline {
  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      // Start listening for submission requests

      val submissionsManager = context.spawn(SubmissionManager(), "biddingSubmissionManager")
      submissionsManager ! BigQuerySubmission(UUID.randomUUID().toString)

      Behaviors.same
    }
}


object BiddingPipelineApplication extends App {
  val log = LoggerFactory.getLogger("BiddingPipelineApplication")
  log.info("Starting bidding pipeline...")

  ActorSystem(BiddingPipeline(), "BiddingPipeline")
}
