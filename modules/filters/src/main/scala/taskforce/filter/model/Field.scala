package taskforce.filter.model

sealed trait Field extends Product with Serializable

object Field {
  final case object CreatedDate extends Field
  final case object UpdatedDate extends Field
}
