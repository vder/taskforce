package taskforce.model

import eu.timepit.refined.types.string.NonEmptyString
import java.time.LocalDateTime
import io.circe.refined._
import io.circe.{Decoder, Encoder}, io.circe.generic.auto._

sealed trait Operator

case object Eq extends Operator
case object Lt extends Operator
case object Gt extends Operator
case object Lteq extends Operator
case object Gteq extends Operator

object Operator {
  implicit val encodeOperator: Encoder[Operator] =
    Encoder.encodeString.contramap {
      case Eq   => "eq"
      case Lt   => "lt"
      case Gt   => "gt"
      case Lteq => "lteq"
      case Gteq => "gteq"
    }
  implicit val decodeOperator: Decoder[Operator] =
    Decoder.decodeString.emap {
      case "eq"   => Right(Eq)
      case "lt"   => Right(Lt)
      case "gt"   => Right(Gt)
      case "lteq" => Right(Lteq)
      case "gteq" => Right(Gteq)
      case other  => Left(s"Invalid operator:$other")
    }
}

sealed trait Status

final case object Active extends Status
final case object Deactive extends Status
final case object All extends Status

object Status {
  implicit val encodeStatus: Encoder[Status] =
    Encoder.encodeString.contramap {
      case Active   => "active"
      case Deactive => "deactive"
      case All      => "all"
    }
  implicit val decodeStatus: Decoder[Status] =
    Decoder.decodeString.emap {
      case "active"   => Right(Active)
      case "deactive" => Right(Deactive)
      case "all"      => Right(All)
      case other      => Left(s"Invalid status: $other")
    }

}

sealed trait Field

case object From extends Field
case object To extends Field

object Field {
  implicit val encodeField: Encoder[Field] =
    Encoder.encodeString.contramap {
      case From => "from"
      case To   => "to"
    }
  implicit val decodeField: Decoder[Field] =
    Decoder.decodeString.emap {
      case "from" => Right(From)
      case "to"   => Right(To)
      case other  => Left(s"Invalid field $other")
    }
}

sealed trait Condition

case class In(names: List[NonEmptyString]) extends Condition
case class Cond(field: Field, op: Operator, date: LocalDateTime)
    extends Condition
case class State(status: Status) extends Condition

case class Filter(conditions: List[Condition])

object In {
  implicit val decodeIn: Decoder[In] =
    Decoder.forProduct1("in")(In.apply)
  implicit val encodeIn: Encoder[In] =
    Encoder.forProduct1("in")(_.names)
}
object Cond {
  implicit val decodeCond: Decoder[Cond] =
    Decoder.forProduct3[Cond, Field, Operator, LocalDateTime](
      "field",
      "op",
      "date"
    )(Cond.apply)
  implicit val encodeCond: Encoder[Cond] =
    Encoder.forProduct3(
      "field",
      "op",
      "date"
    )(x => (x.field, x.op, x.date))
}

object State {
  implicit val decodeState: Decoder[State] =
    Decoder.forProduct1("state")(State.apply)
  implicit val encodeState: Encoder[State] =
    Encoder.forProduct1("state")(_.status)
}
