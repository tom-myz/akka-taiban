package moe.awawa.taiban.model

import akka.actor.ActorRef

object UserModels {
  case class User(name: String, ref: ActorRef)
  case class Combo(user: User, comboNumber: Int)
  case class UserList(users: Seq[User])

  case object Initialize
  case object Terminate
}
