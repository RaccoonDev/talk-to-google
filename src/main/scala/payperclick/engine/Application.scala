package payperclick.engine

import java.util.UUID

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{RunnableGraph, Sink, Source, SubFlow}

import scala.util.Random
import scala.concurrent.duration._


class GoogleBiddingActor() extends Actor with ActorLogging {

  import GoogleBiddingActor._

  val random = new Random()

  override def receive: Receive = {
    case StreamInit =>
      log.info("Initializing stream...")
      sender() ! StreamAck

    case StreamComplete =>
      log.info("Stream complete")
      context.stop(self)

    case StreamFail(ex) =>
      log.warning(s"Stream failed: $ex")

    case req: Vector[BiddingRequest] =>
      log.info(s"Received bidding request vector of size: ${req.size}")
      Thread.sleep(random.between(50, 350))
      sender() ! StreamAck
  }
}

object GoogleBiddingActor {

  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(ex: Throwable)

  case class BiddingRequest(submissionId: UUID, bidId: Long,
                            accountId: Long, adGroupId: Long, keywordId: Long,
                            maxCpc: BigDecimal)

//  def apply(accountId: Long): Props = Props(new GoogleBiddingActor(accountId))
}

object Application extends App {

  implicit val system: ActorSystem = ActorSystem("PayPerClickEngine")
  implicit val materializer: Materializer = Materializer(system)

  import GoogleBiddingActor._

  val googleBiddingActorRef = system.actorOf(Props[GoogleBiddingActor])
  val googleBiddingSink = Sink.actorRefWithBackpressure(
    googleBiddingActorRef,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAck,
    onFailureMessage = throwable => StreamFail(throwable)
  )

  val submissionId = UUID.randomUUID()
  val accountId = 1
  val random = new Random()
  val a = Source(1 to 2)
    .map { i => BiddingRequest(submissionId, i, random.between(1, 3500), random.nextLong(), random.nextLong(), random.nextDouble())}
    .groupBy(maxSubstreams = 3500, _.accountId)
    .groupedWithin(10000, 1.minute)
    .to(googleBiddingSink)
    .run()

}
