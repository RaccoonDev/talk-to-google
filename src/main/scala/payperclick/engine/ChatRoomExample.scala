package payperclick.engine

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}

sealed trait RoomCommand
final case class GetSession(screenName: String, replyTo: ActorRef[SessionEvent]) extends RoomCommand

sealed trait SessionEvent
final case class SessionGranted(handle: ActorRef[PostMessage]) extends SessionEvent
final case class SessionDenied(reason: String) extends SessionEvent
final case class MessagePosted(screenName: String, message: String) extends SessionEvent

trait SessionCommand
final case class PostMessage(message: String) extends SessionCommand
private final case class NotifyClient(message: MessagePosted) extends SessionCommand

object ChatRoom {
  // This one is private to ability to send messages only to
  // chat session within chat room
  private final case class PublishSessionMessage(screenName: String, message: String) extends RoomCommand

  def apply(): Behavior[RoomCommand] =
    chatRoom(List.empty)

  private def chatRoom(sessions: List[ActorRef[SessionCommand]]): Behavior[RoomCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetSession(screenName, client) =>
          // create a child actor for further interaction with the client
          val ses = context.spawn(
            session(context.self, screenName, client),
            name = URLEncoder.encode(screenName, StandardCharsets.UTF_8.name))
          client ! SessionGranted(ses)
          chatRoom(ses :: sessions)
        case PublishSessionMessage(screenName, message) =>
          val notification = NotifyClient(MessagePosted(screenName, message))
          sessions.foreach(_ ! notification)
          Behaviors.same
      }
    }

  private def session(room: ActorRef[PublishSessionMessage], screenName: String, client: ActorRef[SessionEvent]) : Behavior[SessionCommand] =
    Behaviors.receiveMessage {
      case NotifyClient(message) =>
        // published from the room
        client ! message
        Behaviors.same
      case PostMessage(message) =>
        // from client, publish to other via the room
        room ! PublishSessionMessage(screenName, message)
        Behaviors.same
    }
}

object Gabbler {

  def apply(): Behavior[SessionEvent] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage({
        case SessionGranted(handle) =>
          handle ! PostMessage("Hello World!")
          Behaviors.same
        case SessionDenied(reason) =>
          context.log.info(s"session request was denied: $reason")
          Behaviors.stopped
        case MessagePosted(screenName, message) =>
          context.log.info(s"message has been posted by '$screenName': $message")
          Behaviors.stopped
      })
    }
}

object ChatRoomExample {


  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val  chatRoom = context.spawn(ChatRoom(), "chatroom")
      val gabblerRef = context.spawn(Gabbler(), "gabbler")
      context.watch(gabblerRef)

      chatRoom ! GetSession("ol' Gabbler", gabblerRef)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }
}

object ChatRoomApp extends App {
  ActorSystem(ChatRoomExample(), "ChatRoomExample")
}
