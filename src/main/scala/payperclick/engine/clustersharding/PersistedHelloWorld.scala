package payperclick.engine.clustersharding

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.util.Timeout
import payperclick.engine.CborSerializable

import scala.concurrent.Future
import scala.concurrent.duration._

object PersistedHelloWorld {

  // Command
  trait Command extends CborSerializable
  final case class Greet(whom: String)(val replyTo: ActorRef[Greeting]) extends Command
  // Response
  final case class Greeting(whom: String, numberOfPeople: Int) extends CborSerializable

  // Event
  final case class Greeted(whom: String) extends CborSerializable

  // State
  final case class KnownPeople(names: Set[String]) extends CborSerializable {
    def add(name: String): KnownPeople = copy(names = names + name)

    def numberOfPeople: Int = names.size
  }

  private val commandHandler: (KnownPeople, Command) => Effect[Greeted, KnownPeople] = { (_, cmd) =>
    cmd match {
      case cmd: Greet => greet(cmd)
    }
  }

  private def greet(cmd: Greet): Effect[Greeted, KnownPeople] =
    Effect.persist(Greeted(cmd.whom)).thenRun(state => cmd.replyTo ! Greeting (cmd.whom, state.numberOfPeople))

  private val eventHandler: (KnownPeople, Greeted) => KnownPeople = { (state, evt) =>
    state.add(evt.whom)
  }

  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("HelloWorld")

  def apply(entityId: String, persistenceId: PersistenceId): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.info("Starting Hello World {}", entityId)
      EventSourcedBehavior(persistenceId, emptyState = KnownPeople(Set.empty), commandHandler, eventHandler)
    }
  }
}

class HelloWorldService(system: ActorSystem[_]) {
  import system.executionContext

  private val sharding: ClusterSharding = ClusterSharding(system)

  sharding.init(Entity(typeKey = PersistedHelloWorld.TypeKey) { entityContext =>
    PersistedHelloWorld(entityContext.entityId, PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId))
  })

  private implicit val askTimeout: Timeout = Timeout(5.seconds)

  def greet(worldId: String, whom: String): Future[Int] = {
    val entityRef: EntityRef[PersistedHelloWorld.Command] = sharding.entityRefFor(PersistedHelloWorld.TypeKey, worldId)
    val greeting = entityRef ? PersistedHelloWorld.Greet(whom)
    greeting.map(_.numberOfPeople)
  }

}
