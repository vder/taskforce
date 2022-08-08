package taskforce.common.instances

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

import taskforce.common.ResponseError

trait Circe {

  implicit val responseErrorDecoder: Decoder[ResponseError.NotFound] = deriveDecoder
  implicit val responseErrorEncoder: Encoder[ResponseError.NotFound] = deriveEncoder

}
