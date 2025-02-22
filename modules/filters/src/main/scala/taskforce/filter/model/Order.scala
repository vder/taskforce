package taskforce.filter.model

sealed trait Order extends Product with Serializable

object Order {
  case object Asc  extends Order
  case object Desc extends Order
}
