package taskforce.filter.model

sealed trait Operator extends Product with Serializable

object Operator {
  case object Eq   extends Operator
  case object Lt   extends Operator
  case object Gt   extends Operator
  case object Lteq extends Operator
  case object Gteq extends Operator
}
