package payperclick.engine

import java.util.UUID

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.duration._
import scala.util.Random


class GoogleBiddingActor() extends PersistentActor with ActorLogging  {

  import GoogleBiddingActor._

  val random = new Random()

  override def persistenceId: String = "googleBiddingActor_acct_1"

  override def receiveCommand: Receive = {
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
      Thread.sleep(50 + random.nextInt(300))
      persist(req) { e =>
        log.info(s"Persisted event: $e")
        sender() ! StreamAck
      }

  }

  override def receiveRecover: Receive = {
    case _ =>
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
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import GoogleBiddingActor._

  val googleBiddingActorRef = system.actorOf(Props[GoogleBiddingActor])
  val googleBiddingSink = Sink.actorRefWithAck(
    googleBiddingActorRef,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAck,
    onFailureMessage = throwable => StreamFail(throwable)
  )

  val submissionId = UUID.randomUUID()
  val accountId = 1
  val random = new Random()
  Source(1 to 100000) // This source should be substituted with Google PubSub Subscriber
    .map { i => BiddingRequest(submissionId, i, accountId, random.nextLong(), random.nextLong(), random.nextDouble())}
    .groupedWithin(10000, 1.minute)
    .to(googleBiddingSink)
    .run()

}
