package payperclick.engine

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

sealed trait AccountQueueCommand

object AccountQueueManager {
  def accountQueue(): Behavior[AccountQueueCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case _ =>
          context.log.info("I've received the message")
          Behaviors.same
      }
    }
}