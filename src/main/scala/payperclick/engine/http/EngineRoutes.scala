package payperclick.engine.http

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.ClusterEvent.{MemberEvent, MemberWeaklyUp}
import akka.cluster.typed.{Cluster, Subscribe}
import akka.event.Logging
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.{KillSwitches, Materializer, OverflowStrategy, UniqueKillSwitch}
import payperclick.engine.http.entities.JsonSupport
import payperclick.engine.http.entities.SubmissionEntities.Submission

import scala.concurrent.duration._

final class EngineRoutes(system: ActorSystem[_]) extends JsonSupport {

  import akka.http.scaladsl.server.Directives._

  implicit val materializer: Materializer = Materializer(system)

  val cluster: Cluster = Cluster(system)

  val sourceActor: Source[MemberEvent, ActorRef[MemberEvent]] = ActorSource.actorRef[MemberEvent](
    completionMatcher =  { case MemberWeaklyUp(_) if false => },
    failureMatcher = { case _ if false => new RuntimeException("that must never happen, this stream should continue") },
    bufferSize = 16,
    overflowStrategy = OverflowStrategy.dropHead
  )

  val (sink, source) =
    MergeHub.source[Message](perProducerBufferSize = 16).toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both).run()
  source.runWith(Sink.ignore)

  val a: ActorRef[MemberEvent] = sourceActor.map(m => TextMessage.Strict(m.toString) ).to(sink).run()
  cluster.subscriptions ! Subscribe(a, classOf[MemberEvent])

  val busFlow: Flow[Message, Message, UniqueKillSwitch] = Flow.fromSinkAndSource(sink, source)
    .joinMat(KillSwitches.singleBidi[Message, Message])(Keep.right)
    .backpressureTimeout(3.seconds)

  // Here I'm simulating a listener of events from akka cluster
  // this guy is contributing to the pubsub
  val switch: UniqueKillSwitch =
    Source
      .repeat(TextMessage("I'm generating this message"))
      .throttle(1, 2.seconds)
      .viaMat(busFlow)(Keep.right)
      .to(Sink.foreach(println))
      .run()

  val engine: Route =
    path("api") {
      get {
        complete("Hello from api")
      }
    } ~ path("api" / "ws") {
      handleWebSocketMessages(busFlow)
    } ~ path("api" / "submission") {
      post {
        entity(as[Submission]) { submission =>
          // From this place I can start working with submission and trigger
          // appropriate parts of the system to start moving
          println(s"Received submission $submission")
          complete("This is a post request to API. Expecting a triggering of bid submission")
        }
      }
    }
}


