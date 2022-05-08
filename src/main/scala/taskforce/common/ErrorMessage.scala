package taskforce.common

import io.circe.{Decoder, Encoder}
import org.http4s.EntityEncoder
import org.http4s.circe._

case class ErrorMessage(code: String, message: String)
object ErrorMessage {

  implicit val ProjectDecoder: Decoder[ErrorMessage] =
    Decoder.forProduct2("code", "message")(ErrorMessage.apply)
  implicit val ProjectEncoder: Encoder[ErrorMessage] =
    Encoder.forProduct2("code", "message")(p => (p.code, p.message))

  implicit def errMessageEntityEncoder[F[_]]: EntityEncoder[F, ErrorMessage] =
    jsonEncoderOf[F, ErrorMessage]
}
