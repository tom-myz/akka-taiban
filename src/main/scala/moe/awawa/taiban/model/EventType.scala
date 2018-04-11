package moe.awawa.taiban.model

sealed trait EventType {}

object EventType {
  case object Logging extends EventType
  case object NodeJudge extends EventType
}
