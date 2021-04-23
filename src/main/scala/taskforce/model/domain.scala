package taskforce.model

import java.util.UUID
import doobie.util.meta.Meta
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._

object domain {
  case class UserId(id: UUID) extends AnyVal

  object UserId {
    implicit val userIdDecoder: Decoder[UserId] = deriveDecoder[UserId]
    implicit val userIdEncoder: Encoder[UserId] = deriveEncoder[UserId]
  }
}
