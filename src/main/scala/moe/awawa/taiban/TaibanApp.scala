package moe.awawa.taiban

import akka.actor.{ActorSystem, Props}
import moe.awawa.taiban.actor.Live
import moe.awawa.taiban.model.UserModels.Initialize

import scala.concurrent.ExecutionContext

object TaibanApp extends App {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val system = ActorSystem.create("actorSystem")
  system.actorOf(Props(new Live)) ! Initialize
}
