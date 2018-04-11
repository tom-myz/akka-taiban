package moe.awawa.taiban.model

object UserActorStatus {
  sealed trait Status
  case object Created extends Status
  case object Ready extends Status
}
