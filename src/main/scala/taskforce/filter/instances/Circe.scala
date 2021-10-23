package taskforce.filter.instances

import cats.syntax.functor._
import io.circe.generic.semiauto._
import io.circe.refined._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import java.time.LocalDateTime
import java.util.UUID
import taskforce.filter._

trait Circe extends taskforce.task.instances.Circe {

  implicit lazy val encodeCriteria: Encoder[Criteria] = Encoder.instance {
    case in @ In(_)                   => in.asJson
    case state @ State(_)             => state.asJson
    case task @ TaskCreatedDate(_, _) => task.asJson
  }

  implicit lazy val decodeCriteria: Decoder[Criteria] =
    List[Decoder[Criteria]](
      Decoder[In].widen,
      Decoder[State].widen,
      Decoder[TaskCreatedDate].widen
    ).reduceLeft(_ or _)

  implicit lazy val encodeOperator: Encoder[Operator] =
    Encoder.encodeString.contramap(helpers.operatorToString)

  implicit lazy val decodeOperator: Decoder[Operator] =
    Decoder.decodeString.emap { x =>
      helpers.operatorFromString
        .lift(x)
        .toRight(s"invalid operator: $x")
    }
  implicit lazy val encodeStatus: Encoder[Status] =
    Encoder.encodeString.contramap(helpers.statusToString)

  implicit lazy val decodeStatus: Decoder[Status] =
    Decoder.decodeString.emap { x =>
      helpers.statusFromString
        .lift(x)
        .toRight(s"invalid status: $x")
    }

  implicit lazy val decodeTaskCreatedDate: Decoder[TaskCreatedDate] =
    Decoder.forProduct2[TaskCreatedDate, Operator, LocalDateTime](
      "op",
      "date"
    )(TaskCreatedDate.apply)
  implicit lazy val encodeTaskCreatedDate: Encoder[TaskCreatedDate] =
    Encoder.forProduct2(
      "op",
      "date"
    )(x => (x.op, x.date))

  implicit lazy val decodeState: Decoder[State] =
    Decoder.forProduct1("state")(State.apply)
  implicit lazy val encodeState: Encoder[State] =
    Encoder.forProduct1("state")(_.status)

  implicit lazy val decodeFilterId: Decoder[FilterId] =
    Decoder[UUID].map(FilterId.apply)
  implicit lazy val encodeFilterId: Encoder[FilterId] =
    Encoder[UUID].contramap(_.value)

  implicit lazy val decodeFilter: Decoder[Filter] =
    Decoder.forProduct2("id", "conditions")(Filter.apply)
  implicit lazy val encodeFilter: Encoder[Filter] =
    Encoder.forProduct2("id", "conditions")(x => (x.id, x.conditions))

  implicit lazy val decodeNewFilter: Decoder[NewFilter] =
    Decoder.forProduct1("conditions")(NewFilter.apply)
  implicit lazy val encodeNewFilter: Encoder[NewFilter] =
    Encoder.forProduct1("conditions")(_.conditions)

  implicit lazy val circeRowDecoder: Decoder[FilterResultRow] =
    deriveDecoder[FilterResultRow]
  implicit lazy val circeRowEncoder: Encoder[FilterResultRow] =
    deriveEncoder[FilterResultRow]

  implicit lazy val decodeIn: Decoder[In] =
    Decoder.forProduct1("in")(In.apply)
  implicit lazy val encodeIn: Encoder[In] =
    Encoder.forProduct1("in")(_.names)

}
