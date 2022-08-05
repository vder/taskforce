package taskforce.filter.instances

import cats.syntax.either._
import cats.syntax.functor._
import io.circe.refined._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import java.util.UUID
import taskforce.filter._
import io.circe.generic.semiauto._

trait Circe {

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
      helpers.operatorFromString(x).asRight
    }
  implicit lazy val encodeStatus: Encoder[Status] =
    Encoder.encodeString.contramap(helpers.statusToString)

  implicit lazy val decodeStatus: Decoder[Status] =
    Decoder.decodeString.emap { x =>
      helpers.statusFromString(x).asRight
    }

  implicit lazy val decodeTaskCreatedDate: Decoder[TaskCreatedDate] = deriveDecoder
  implicit lazy val encodeTaskCreatedDate: Encoder[TaskCreatedDate] = deriveEncoder

  implicit lazy val decodeState: Decoder[State] = deriveDecoder
  implicit lazy val encodeState: Encoder[State] = deriveEncoder

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

  implicit lazy val decodeIn: Decoder[In] =
    Decoder.forProduct1("in")(In.apply)
  implicit lazy val encodeIn: Encoder[In] =
    Encoder.forProduct1("in")(_.names)

}
