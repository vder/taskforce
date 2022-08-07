package taskforce.filter.model

sealed trait Operator extends Product with Serializable

object Operator {
  final case object Eq   extends Operator
  final case object Lt   extends Operator
  final case object Gt   extends Operator
  final case object Lteq extends Operator
  final case object Gteq extends Operator
}
