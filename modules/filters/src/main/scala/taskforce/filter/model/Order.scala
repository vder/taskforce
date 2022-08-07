package taskforce.filter.model

sealed trait Order

object Order {
  final case object Asc  extends Order
  final case object Desc extends Order
}
