package payperclick.engine.http

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.Message
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Source}

trait ChatMessage
case class UserJoined(name: String, actorRef: ActorRef) extends ChatMessage
case class UserLeft(name: String) extends ChatMessage
case class IncomingMessage(msg: ChatMessage) extends ChatMessage
case class SystemMessage(msg: String) extends ChatMessage

class ChatRoom(roomId: Int, actorSystem: ActorSystem) {
  private[this] val chatRoomActor = actorSystem.actorOf(Props(classOf[ChatRoomActor], roomId))

  def websocketFlow(user: String): Flow[Message, Message, _] = ???

  def sendMessage(message: ChatMessage): Unit = chatRoomActor ! message
}

object ChatRoom {
  def apply(roomId: Int)(implicit actorSystem: ActorSystem) = new ChatRoom(roomId, actorSystem)
}

object ChatRooms {
  var chatRooms: Map[Int, ChatRoom] = Map.empty[Int, ChatRoom]

  def findOrCreate(number: Int)(implicit actorSystem: ActorSystem): ChatRoom = chatRooms.getOrElse(number, createNewChatRoom(number))

  private def createNewChatRoom(number: Int)(implicit actorSystem: ActorSystem): ChatRoom = {
    val chatRoom = ChatRoom(number)
    chatRooms += number -> chatRoom
    chatRoom
  }
}

class ChatRoomActor(roomId: Int) extends Actor {
  var participants: Map[String, ActorRef] = Map.empty[String, ActorRef]

  override def receive: Receive = {
    case UserJoined(name, actorRef) =>
      participants += name -> actorRef
      broadcast(SystemMessage(s"User $name joined channel..."))
      println(s"User $name joined channel [$roomId]")
    case UserLeft(name) =>
      println(s"User $name left channel[$roomId]")
      broadcast(SystemMessage(s"User $name left channel[$roomId]"))
      participants -= name

    case msg: IncomingMessage =>
      broadcast(msg)
  }

  def broadcast(message: ChatMessage): Unit = participants.values.foreach(_ ! message)
}