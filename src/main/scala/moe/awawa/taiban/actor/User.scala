package moe.awawa.taiban.actor

import akka.actor.{Actor, ActorSystem}
import moe.awawa.taiban.`trait`.EventLooper
import moe.awawa.taiban.enrich.RichString._
import moe.awawa.taiban.model.EventType.{Logging, NodeJudge}
import moe.awawa.taiban.model.UserModels.{
  Combo,
  Terminate,
  UserList,
  User => UserWrapper
}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class User(val name: String)(implicit val ec: ExecutionContext)
    extends Actor
    with EventLooper {

  import User._
  import moe.awawa.taiban.model.UserActorStatus._

  override val actorSystem: ActorSystem = context.system

  val logger = LoggerFactory.getLogger(s"moe.awawa.taiban.actor.User.${name}")
  val accuracy = math.random() * accuracyNoise + (1 - accuracyNoise)

  private[this] var combo = 0
  private[this] var otherUsers: Map[UserWrapper, Int] = Map()
  private[this] var status: Status = Created

  override def receive: Receive = {
    case list: UserList => {
      otherUsers = list.users
        .filterNot(_.ref == self)
        .map(_ -> 0)
        .toMap
      status = Ready
      context.become(activate)

      kickEvent(Logging, comboUpdateInterval)(write)
      kickEvent(NodeJudge, frequency)(tap)
    }
  }

  private def activate: Receive = {
    case c: Combo => {
      otherUsers -= c.user
      otherUsers += c.user -> c.comboNumber
    }
    case Terminate => {
      cancelAll
      write()
      context.stop(self)
    }
  }

  protected def write = () => logger.debug(formattedCombo)

  protected def tap = () => {
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
