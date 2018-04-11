package moe.awawa.taiban.`trait`

import akka.actor.{ActorSystem, Cancellable}
import moe.awawa.taiban.model.EventType

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

trait EventLooper {
  val actorSystem: ActorSystem
  implicit val ec: ExecutionContext

  protected val events: mutable.Map[EventType, Cancellable] = mutable.Map()

  protected def kickEvent[A](eventType: EventType, interval: FiniteDuration)(
      procedure: () => A): A = {
    Try(
      actorSystem.scheduler.scheduleOnce(interval)(
        kickEvent(eventType, interval)(procedure))).map { fb =>
      events += (eventType -> fb)
    }
    procedure()
  }

  protected def cancelAll: Boolean = {
    events
      .map { case (_, value) => value.cancel() }
      .reduce((l, r) => l && r)
  }
}
