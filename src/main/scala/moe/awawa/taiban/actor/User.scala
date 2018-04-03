package moe.awawa.taiban.actor

import akka.actor.{Actor, Cancellable}
import moe.awawa.taiban.enrich.RichString._
import moe.awawa.taiban.model.UserModels.{
  Combo,
  Terminate,
  UserList,
  User => UserWrapper
}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class User(val name: String)(implicit val ec: ExecutionContext) extends Actor {

  import User._

  val logger = LoggerFactory.getLogger(s"moe.awawa.taiban.actor.User.${name}")
  var otherUsers: Map[UserWrapper, Int] = Map()
  var status: Status = Created

  var logEvent: Cancellable = new Cancellable {
    override def cancel(): Boolean = true
    override def isCancelled: Boolean = false
  }

  override def receive: Receive = {
    case list: UserList => {
      otherUsers = list.users
        .filterNot(_.ref == self)
        .map(_ -> 0)
        .toMap
      status = Ready
      context.become(activate)
      context.system.scheduler.scheduleOnce(5 seconds)(write)
    }
  }

  private def activate: Receive = {
    case c: Combo => {
      otherUsers -= c.user
      otherUsers += c.user -> c.comboNumber
    }
    case Terminate => {
      logEvent.cancel()
      context.stop(self)
    }
  }

  protected def write: Unit = {
    Try(context.system.scheduler.scheduleOnce(5 seconds)(write)).map { fb =>
      logEvent = fb
    }
    logger.debug(formattedCombo)
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
  sealed trait Status
  case object Created extends Status
  case object Ready extends Status
}
