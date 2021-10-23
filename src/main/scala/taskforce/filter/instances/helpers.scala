package taskforce.filter.instances

import taskforce.filter._

object helpers {

  val operatorFromString: PartialFunction[String, Operator] = {
    case "eq"   => Eq
    case "lt"   => Lt
    case "gt"   => Gt
    case "lteq" => Lteq
    case "gteq" => Gteq
  }

  val operatorToString: PartialFunction[Operator, String] = {
    case Eq   => "eq"
    case Gt   => "gt"
    case Gteq => "gteq"
    case Lt   => "lt"
    case Lteq => "lteq"
  }

  val statusFromString: PartialFunction[String, Status] = {
    case "active"   => Active
    case "deactive" => Deactive
    case "all"      => All
  }

  val statusToString: PartialFunction[Status, String] = {
    case Active   => "active"
    case Deactive => "deactive"
    case All      => "all"
  }
}
