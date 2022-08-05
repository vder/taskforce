package taskforce.filter.instances

import taskforce.filter._

object helpers {

  val operatorFromString: String => Operator = {
    case "eq"   => Eq
    case "lt"   => Lt
    case "gt"   => Gt
    case "lteq" => Lteq
    case "gteq" => Gteq
  }

  val operatorToString: Operator => String = {
    case Eq   => "eq"
    case Gt   => "gt"
    case Gteq => "gteq"
    case Lt   => "lt"
    case Lteq => "lteq"
  }

  val statusFromString: String => Status = {
    case "active"   => Active
    case "deactive" => Deactive
    case "all"      => All
  }

  val statusToString: Status => String = {
    case Active   => "active"
    case Deactive => "deactive"
    case All      => "all"
  }
}
