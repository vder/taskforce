package taskforce.common

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s.EntityEncoder
import org.http4s.circe._

case class ErrorMessage(code: String, message: String)
object ErrorMessage {

  implicit val ProjectDecoder: Decoder[ErrorMessage] =
    deriveDecoder[ErrorMessage]
  implicit val ProjectEncoder: Encoder[ErrorMessage] =
    deriveEncoder[ErrorMessage]

  implicit def errMessageEntityEncoder[F[_]]: EntityEncoder[F, ErrorMessage] = jsonEncoderOf[F, ErrorMessage]
}
