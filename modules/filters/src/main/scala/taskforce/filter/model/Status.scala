package taskforce.filter.model

sealed trait Status extends Product with Serializable

object Status {

  final case object Active   extends Status
  final case object Inactive extends Status
  final case object All      extends Status
}
