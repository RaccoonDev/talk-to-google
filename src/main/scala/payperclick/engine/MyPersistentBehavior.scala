package payperclick.engine

import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

sealed trait Command
final case class Add(data: String) extends Command
case object PrintState extends Command
case object Clear extends Command

sealed trait Event
final case class Added(data: String) extends Event
case object Cleared extends Event

object MyPersistentBehavior {

  final case class State(history: List[String] = Nil)

  val commandHandler: (State, Command) => Effect[Event, State] = (state, command) =>
    command match {
      case Add(data) => Effect.persist(Added(data))
      case Clear => Effect.persist(Cleared)
      case PrintState => {
        println(state)
        Effect.none
      }
    }

  val eventHandler: (State, Event) => State = { (state, event) =>
    event match {
      case Added(data) =>
        println(s"Restored added event with data: $data")
        state.copy((data :: state.history).take(5))
      case Cleared =>
        println("Restored cleared event")
        State(Nil)
    }
  }

  def apply(): Behavior[Command] =
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("abc"),
      emptyState = State(Nil),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}
