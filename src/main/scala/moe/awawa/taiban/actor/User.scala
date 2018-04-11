package moe.awawa.taiban.actor

import akka.actor.{Actor, Cancellable}
import moe.awawa.taiban.enrich.RichString._
import moe.awawa.taiban.model.EventType
import moe.awawa.taiban.model.EventType.{Logging, NodeJudge}
import moe.awawa.taiban.model.UserModels.{
  Combo,
  Terminate,
  UserList,
  User => UserWrapper
}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class User(val name: String)(implicit val ec: ExecutionContext) extends Actor {

  import moe.awawa.taiban.model.UserActorStatus._
  import User._

  val logger = LoggerFactory.getLogger(s"moe.awawa.taiban.actor.User.${name}")
  val accuracy = math.random() * accuracyNoise + (1 - accuracyNoise)

  private[this] var combo = 0
  private[this] var otherUsers: Map[UserWrapper, Int] = Map()
  private[this] var status: Status = Created

  protected val events: mutable.Map[EventType, Cancellable] = mutable.Map()

  override def receive: Receive = {
    case list: UserList => {
      otherUsers = list.users
        .filterNot(_.ref == self)
        .map(_ -> 0)
        .toMap
      status = Ready
      context.become(activate)
      context.system.scheduler.scheduleOnce(comboUpdateInterval)(write)
      context.system.scheduler.scheduleOnce(frequency)(tap)
    }
  }

  private def activate: Receive = {
    case c: Combo => {
      otherUsers -= c.user
      otherUsers += c.user -> c.comboNumber
    }
    case Terminate => {
      events.foreach { case (_, value) => value.cancel() }
      logger.debug(formattedCombo)
      context.stop(self)
    }
  }

  protected def write: Unit = {
    Try(context.system.scheduler.scheduleOnce(comboUpdateInterval)(write)).map { fb =>
      events += (Logging -> fb)
    }
    logger.debug(formattedCombo)
  }

  protected def tap: Unit = {
    Try(context.system.scheduler.scheduleOnce(frequency)(tap)).map { fb =>
      events += (NodeJudge -> fb)
    }
    combo = if (math.random() < accuracy) (combo + 1) else 0
    context.parent ! Combo(UserWrapper(name, self), combo)
  }

  protected def formattedCombo: String = {
    otherUsers.toSeq
      .map {
        case (k, v) => s"${k.name.extend(16)}:\t ${v} Combo!"
      }
      .foldLeft("")((left, right) => left + "\n" + right)
  }

}

object User {
  val accuracyNoise = 0.05
  val frequency = 200 milliseconds
  val comboUpdateInterval = 2 seconds
}
