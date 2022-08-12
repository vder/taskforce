package taskforce.filter.model

sealed trait Order extends Product with Serializable

object Order {
  final case object Asc  extends Order
  final case object Desc extends Order
}
