package payperclick.engine.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}

final class EngineRoutes(system: ActorSystem[_]) {

  import akka.http.scaladsl.server.Directives._
  implicit val materializer: Materializer = Materializer(system)

  def greeter: Flow[Message, Message, Any] =
    Flow[Message].mapConcat {
      case tm: TextMessage =>

        TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
      case bm: BinaryMessage =>
        // ignore binary messages but drain content to avoid the stream being clogged
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }

  val engine: Route =
    path("api") {
      get {
        complete("Hello from api")
      }
    } ~ path("api" / "ws") {
      handleWebSocketMessages(greeter)
    }
}
