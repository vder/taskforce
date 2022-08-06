package taskforce.filter.instances

import taskforce.filter.model._

object helpers {

  val operatorFromString: String => Operator = {
    case "eq"   => Operator.Eq
    case "lt"   => Operator.Lt
    case "gt"   => Operator.Gt
    case "lteq" => Operator.Lteq
    case "gteq" => Operator.Gteq
  }

  val operatorToString: Operator => String = {
    case Operator.Eq   => "eq"
    case Operator.Gt   => "gt"
    case Operator.Gteq => "gteq"
    case Operator.Lt   => "lt"
    case Operator.Lteq => "lteq"
  }

  val statusFromString: String => Status = {
    case "active"   => Status.Active
    case "deactive" => Status.Deactive
    case "all"      => Status.All
  }

  val statusToString: Status => String = {
    case Status.Active   => "active"
    case Status.Deactive => "deactive"
    case Status.All      => "all"
  }
}
