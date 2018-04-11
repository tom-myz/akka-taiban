package moe.awawa.taiban.actor

import akka.actor.{Actor, Props}
import moe.awawa.taiban.model.UserModels.{Combo, Initialize, Terminate, UserList, User => UserWrapper}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class Live(implicit val ec: ExecutionContext) extends Actor {

  val logger = LoggerFactory.getLogger(this.getClass)

  val users: Seq[UserWrapper] = Seq(
    create("新人スタッフ"),
    create("ポプ子"),
    create("April@こころ激推し")
  )

  private def create(name: String): UserWrapper = {
    UserWrapper(name, context.actorOf(Props(classOf[User], name, ec)))
  }

  private def broadcastCombo(c: Combo) = {
    users.filterNot(_.ref == c.user.ref)
      .map(_.ref)
      .foreach(_ ! c)
  }

  override def receive: Receive = {
    case Initialize => {
      logger.info("ライブを開始します。")
      context.system.scheduler.scheduleOnce(20 seconds){
        logger.info("ライブ終了です。")
        users.foreach(_.ref ! Terminate)
        context.stop(self)
      }
      users.foreach(_.ref ! UserList(users))
    }
    case c: Combo => broadcastCombo(c)
    case x => logger.info(x.getClass.getCanonicalName)
  }

  override def postStop(): Unit = {
    context.system.terminate()
  }

  context.setReceiveTimeout(1 minute)
}
