package taskforce.filter.instances

import cats.syntax.either._
import cats.syntax.functor._
import io.circe.refined._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import taskforce.filter.model._
import monix.newtypes.integrations.DerivedCirceCodec
import taskforce.common.instances
import java.time.Instant
import java.util.UUID

trait Circe extends DerivedCirceCodec with instances.Circe {

  implicit lazy val encodeCriteria: Encoder[Criteria] = Encoder.instance {
    case in @ Criteria.In(_)                   => in.asJson
    case state @ Criteria.State(_)             => state.asJson
    case task @ Criteria.TaskCreatedDate(_, _) => task.asJson
  }

  implicit lazy val decodeCriteria: Decoder[Criteria] =
    List[Decoder[Criteria]](
      Decoder[Criteria.In].widen,
      Decoder[Criteria.State].widen,
      Decoder[Criteria.TaskCreatedDate].widen
    ).reduceLeft(_ or _)

  implicit lazy val encodeOperator: Encoder[Operator] =
    Encoder.encodeString.contramap(helpers.operatorToString)

  implicit lazy val decodeOperator: Decoder[Operator] =
    Decoder.decodeString.emap { x =>
      helpers.operatorFromString(x).asRight
    }
  implicit lazy val encodeStatus: Encoder[Status] =
    Encoder.encodeString.contramap(helpers.statusToString)

  implicit lazy val decodeStatus: Decoder[Status] =
    Decoder.decodeString.emap { x =>
      helpers.statusFromString(x).asRight
    }

  implicit lazy val decodeTaskCreatedDate: Decoder[Criteria.TaskCreatedDate] =
    Decoder.forProduct2[Criteria.TaskCreatedDate, Operator, Instant](
      "op",
      "date"
    )(Criteria.TaskCreatedDate.apply)
  implicit lazy val encodeTaskCreatedDate: Encoder[Criteria.TaskCreatedDate] =
    Encoder.forProduct2(
      "op",
      "date"
    )(x => (x.op, x.date))

  implicit lazy val decodeState: Decoder[Criteria.State] =
    Decoder.forProduct1("state")(Criteria.State.apply)
  implicit lazy val encodeState: Encoder[Criteria.State] =
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

  implicit lazy val decodeIn: Decoder[Criteria.In] =
    Decoder.forProduct1("in")(Criteria.In.apply)
  implicit lazy val encodeIn: Encoder[Criteria.In] =
    Encoder.forProduct1("in")(_.names)

  implicit lazy val circeRowDecoder: Decoder[FilterResultRow] = deriveDecoder
  implicit lazy val circeRowEncoder: Encoder[FilterResultRow] = deriveEncoder

}
