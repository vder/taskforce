package taskforce.common.instances

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import taskforce.common.ErrorMessage

trait Circe {

  implicit val ProjectDecoder: Decoder[ErrorMessage] = deriveDecoder
  implicit val ProjectEncoder: Encoder[ErrorMessage] = deriveEncoder

}
