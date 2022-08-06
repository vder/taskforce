package taskforce.filter.instances

import cats.syntax.either._
import cats.syntax.functor._
import io.circe.refined._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import java.util.UUID
import io.circe.generic.semiauto._
import taskforce.filter.model._

trait Circe {

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

  implicit lazy val decodeTaskCreatedDate: Decoder[Criteria.TaskCreatedDate] = deriveDecoder
  implicit lazy val encodeTaskCreatedDate: Encoder[Criteria.TaskCreatedDate] = deriveEncoder

  implicit lazy val decodeState: Decoder[Criteria.State] = deriveDecoder
  implicit lazy val encodeState: Encoder[Criteria.State] = deriveEncoder

  implicit lazy val decodeFilterId: Decoder[FilterId] =
    Decoder[UUID].map(FilterId.apply)
  implicit lazy val encodeFilterId: Encoder[FilterId] =
    Encoder[UUID].contramap(_.value)

  implicit lazy val decodeFilter: Decoder[Filter] = deriveDecoder
  implicit lazy val encodeFilter: Encoder[Filter] = deriveEncoder

  implicit lazy val decodeNewFilter: Decoder[NewFilter] = deriveDecoder
  implicit lazy val encodeNewFilter: Encoder[NewFilter] = deriveEncoder

  implicit lazy val circeRowDecoder: Decoder[FilterResultRow] = deriveDecoder
  implicit lazy val circeRowEncoder: Encoder[FilterResultRow] = deriveEncoder

  implicit lazy val decodeIn: Decoder[Criteria.In] =
    Decoder.forProduct1("in")(Criteria.In.apply)
  implicit lazy val encodeIn: Encoder[Criteria.In] =
    Encoder.forProduct1("in")(_.names)

}
