package taskforce.model

import eu.timepit.refined.types.string.NonEmptyString
import java.time.LocalDateTime
import io.circe.refined._
import io.circe.{Decoder, Encoder}, io.circe.generic.auto._
import java.util.UUID
import cats.implicits._
import doobie.util.meta.Meta
import cats.data.NonEmptyList
import io.circe.generic.semiauto._
import doobie.util.fragment.Fragment
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._

sealed trait Operator {
  override def toString =
    this match {
      case Eq   => "eq"
      case Gt   => "gt"
      case Gteq => "gteq"
      case Lt   => "lt"
      case Lteq => "lteq"
    }
  def toSql =
    this match {
      case Eq   => "="
      case Gt   => ">"
      case Gteq => ">="
      case Lt   => "<"
      case Lteq => "<="
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

  val fromSql: PartialFunction[String, Operator] = {
    case "="  => Eq
    case "<"  => Lt
    case ">"  => Gt
    case "<=" => Lteq
    case ">=" => Gteq
  }

  implicit val operatorMeta: Meta[Operator] =
    Meta[String].imap(fromSql)(_.toSql)

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

sealed trait Criteria {
  def toSql: Fragment
}

case class In(names: List[NonEmptyString]) extends Criteria {

  implicit val purNonEmptyList: Put[List[NonEmptyString]] =
    Put[List[String]].contramap(_.map(_.value))

  override def toSql: Fragment =
    NonEmptyList
      .fromList(names)
      .map(x => Fragments.in(fr"p.name", x))
      .getOrElse(fr"")
}
case class TaskCreatedDate(op: Operator, date: LocalDateTime) extends Criteria {

  override def toSql: Fragment = fr""" t.started ${op.toSql}  ${date}""""

}
case class State(status: Status) extends Criteria {

  override def toSql: Fragment =
    status match {
      case All      => fr"1=1"
      case Deactive => fr"""t.deleted is not null"""
      case Active   => fr"""t.deleted is null"""
    }

}

object In {
  implicit val decodeIn: Decoder[In] =
    Decoder.forProduct1("in")(In.apply)
  implicit val encodeIn: Encoder[In] =
    Encoder.forProduct1("in")(_.names)
}
object TaskCreatedDate {

  def of(op: String, date: LocalDateTime) =
    Operator.fromString.lift(op).map(TaskCreatedDate(_, date))

  implicit val decodeTaskCreatedDate: Decoder[TaskCreatedDate] =
    Decoder.forProduct2[TaskCreatedDate, Operator, LocalDateTime](
      "op",
      "date"
    )(TaskCreatedDate.apply)
  implicit val encodeTaskCreatedDate: Encoder[TaskCreatedDate] =
    Encoder.forProduct2(
      "op",
      "date"
    )(x => (x.op, x.date))
}

object State {
  implicit val decodeState: Decoder[State] =
    Decoder.forProduct1("state")(State.apply)
  implicit val encodeState: Encoder[State] =
    Encoder.forProduct1("state")(_.status)
}

final case class FilterId(value: UUID) extends ResourceId[UUID]

case class NewFilter(conditions: List[Criteria])
case class Filter(id: FilterId, conditions: List[Criteria])

object Filter {

  implicit val filterDecoder: Decoder[Filter] =
    deriveDecoder[Filter]
  implicit val filterEncoder: Encoder[Filter] =
    deriveEncoder[Filter]
}

object NewFilter {

  implicit val filterNewDecoder: Decoder[NewFilter] =
    deriveDecoder[NewFilter]
  implicit val filterNewEncoder: Encoder[NewFilter] =
    deriveEncoder[NewFilter]
}
