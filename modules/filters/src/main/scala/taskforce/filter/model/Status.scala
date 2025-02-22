package taskforce.filter.model

sealed trait Status extends Product with Serializable

object Status {

  case object Active   extends Status
  case object Inactive extends Status
  case object All      extends Status
}
