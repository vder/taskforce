package taskforce.filter.model

sealed trait Field

object Field {
  final case object CreatedDate extends Field
  final case object UpdatedDate extends Field
}
