package taskforce.model

import eu.timepit.refined.types.string.NonEmptyString
import java.time.LocalDateTime
import io.circe.refined._
import io.circe.{Decoder, Encoder}, io.circe.generic.auto._
import java.util.UUID
import cats.implicits._
import doobie.util.meta.Meta

sealed trait Operator {
  override def toString =
    this match {
      case Eq   => "eq"
      case Gt   => "gt"
      case Gteq => "gteq"
      case Lt   => "lt"
      case Lteq => "lteq"
    }
}

case object Eq extends Operator
case object Lt extends Operator
case object Gt extends Operator
case object Lteq extends Operator
case object Gteq extends Operator

object Operator {

  val fromString: PartialFunction[String, Operator] = {
    case "eq"   => Eq
    case "lt"   => Lt
    case "gt"   => Gt
    case "lteq" => Lteq
    case "gteq" => Gteq
  }

  implicit val operatorMeta: Meta[Operator] =
    Meta[String].imap(fromString)(_.toString)

  implicit val encodeOperator: Encoder[Operator] =
    Encoder.encodeString.contramap(_.toString())

  implicit val decodeOperator: Decoder[Operator] =
    Decoder.decodeString.emap { x =>
      fromString
        .lift(x)
        .toRight(s"invalid operator: $x")
    }
}
sealed trait Status {
  override def toString =
    this match {
      case Active   => "active"
      case Deactive => "deactive"
      case All      => "all"
    }
}

final case object Active extends Status
final case object Deactive extends Status
final case object All extends Status

object Status {

  val fromString: PartialFunction[String, Status] = {
    case "active"   => Active
    case "deactive" => Deactive
    case "all"      => All
  }

  implicit val encodeStatus: Encoder[Status] =
    Encoder.encodeString.contramap(_.toString())

  implicit val decodeStatus: Decoder[Status] =
    Decoder.decodeString.emap { x =>
      fromString
        .lift(x)
        .toRight(s"invalid status: $x")
    }

  implicit val operatorMeta: Meta[Status] =
    Meta[String].imap(fromString)(_.toString)

}

sealed trait Field {
  override def toString =
    this match {
      case From => "from"
      case To   => "to"
    }
}

case object From extends Field
case object To extends Field

object Field {

  val fromString: PartialFunction[String, Field] = {
    case "from" => From
    case "to"   => To
  }

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
  implicit val operatorMeta: Meta[Field] =
    Meta[String].imap(fromString)(_.toString)

}

sealed trait Criteria

case class In(names: List[NonEmptyString]) extends Criteria
case class Cond(field: Field, op: Operator, date: LocalDateTime)
    extends Criteria
case class State(status: Status) extends Criteria

final case class FilterId(value: UUID) extends ResourceId[UUID]

case class Filter(id: FilterId, conditions: List[Criteria])

object In {
  implicit val decodeIn: Decoder[In] =
    Decoder.forProduct1("in")(In.apply)
  implicit val encodeIn: Encoder[In] =
    Encoder.forProduct1("in")(_.names)
}
object Cond {

  def of(field: String, op: String, date: LocalDateTime) =
    for {
      f <- Field.fromString.lift(field)
      o <- Operator.fromString.lift(op)
    } yield Cond(f, o, date)

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
