package taskforce.model

import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._
import io.circe.{Decoder, Encoder}, io.circe.generic.auto._
import java.time.LocalDateTime
import java.util.UUID
import doobie.util.meta.Meta
import cats.data.NonEmptyList
import io.circe.generic.semiauto._
import doobie.util.fragment.Fragment
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import cats.syntax.functor._
import io.circe.syntax._

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

case object Eq   extends Operator
case object Lt   extends Operator
case object Gt   extends Operator
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

final case object Active   extends Status
final case object Deactive extends Status
final case object All      extends Status

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

sealed trait Criteria extends Product with Serializable {
  def toSql: Fragment

  def filter(r: Row): Boolean =
    this match {
      case In(names)                        => names.contains(r.projectName)
      case State(Active)                    => r.taskDeleted.isEmpty
      case State(Deactive)                  => r.taskDeleted.isDefined
      case State(All)                       => true
      case cond @ TaskCreatedDate(op, date) => r.taskCreated.map(d => cond.filterByOp(d)).getOrElse(true)
    }
}

object Criteria {

  implicit val encodeCriteria: Encoder[Criteria] = Encoder.instance {
    case in @ In(_)                   => in.asJson
    case state @ State(_)             => state.asJson
    case task @ TaskCreatedDate(_, _) => task.asJson
  }

  implicit val decodeCriteria: Decoder[Criteria] =
    List[Decoder[Criteria]](
      Decoder[In].widen,
      Decoder[State].widen,
      Decoder[TaskCreatedDate].widen
    ).reduceLeft(_ or _)

}

case class In(names: List[NonEmptyString]) extends Criteria {

  implicit val putNonEmptyList: Put[List[NonEmptyString]] =
    Put[List[String]].contramap(_.map(_.value))

  override def toSql: Fragment =
    NonEmptyList
      .fromList(names)
      .map(x => Fragments.in(fr"p.name", x))
      .getOrElse(fr"")
}
case class TaskCreatedDate(op: Operator, date: LocalDateTime) extends Criteria {

  override def toSql: Fragment = Fragment.const(s" t.started ${op.toSql}") ++ fr"${date}"

  val filterByOp: LocalDateTime => Boolean =
    this.op match {
      case Eq   => d => d.isEqual(date)
      case Gt   => d => d.isAfter(date)
      case Gteq => d => d.isAfter(date) || d.isEqual(date)
      case Lt   => d => d.isBefore(date)
      case Lteq => d => d.isBefore(date) || d.isEqual(date)
    }

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

object FilterId {

  implicit val decodeFilterId: Decoder[FilterId] =
    Decoder[UUID].map(FilterId.apply)
  implicit val encodeFilterId: Encoder[FilterId] =
    Encoder[UUID].contramap(_.value)
}

case class NewFilter(conditions: List[Criteria])
case class Filter(id: FilterId, conditions: List[Criteria])

object Filter {

  implicit val decodeFilter: Decoder[Filter] =
    Decoder.forProduct2("id", "conditions")(Filter.apply)
  implicit val encodeFilter: Encoder[Filter] =
    Encoder.forProduct2("id", "conditions")(x => (x.id, x.conditions))
}

object NewFilter {

  implicit val decodeNewFilter: Decoder[NewFilter] =
    Decoder.forProduct1("conditions")(NewFilter.apply)
  implicit val encodeNewFilter: Encoder[NewFilter] =
    Encoder.forProduct1("conditions")(_.conditions)
}

case class Row(
    projectId: Long,
    projectName: String,
    projectCreated: LocalDateTime,
    projectDeleted: Option[LocalDateTime],
    projectAuthor: UUID,
    taskComment: Option[String],
    taskCreated: Option[LocalDateTime],
    taskDeleted: Option[LocalDateTime],
    duration: Option[Long]
)

object Row {

  def fromTuple(x: (Project, Option[Task])) =
    x match {
      case (p, tOpt) =>
        Row(
          p.id.value,
          p.name.value,
          p.created,
          p.deleted,
          p.author.value,
          tOpt.flatMap(_.comment).map(_.value),
          tOpt.map(_.created),
          tOpt.flatMap(_.deleted),
          tOpt.map(_.duration.value.toMinutes())
        )
    }

  implicit val circeRowDecoder: Decoder[Row] =
    deriveDecoder[Row]
  implicit val circeRowEncoder: Encoder[Row] =
    deriveEncoder[Row]
}
