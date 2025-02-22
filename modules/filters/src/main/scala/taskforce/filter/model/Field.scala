package taskforce.filter.model

sealed trait Field extends Product with Serializable

object Field {
  case object CreatedDate extends Field
  case object UpdatedDate extends Field
}
