package taskforce.model

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._
import java.util.UUID

final case class UserId(id: UUID) extends AnyVal

object UserId {
  implicit val userIdDecoder: Decoder[UserId] =
    Decoder[UUID].map(UserId.apply)
  implicit val userIdEncoder: Encoder[UserId] =
    Encoder[UUID].contramap(_.id)
}

final case class User(id: UserId)

object User {
  implicit val ProjectDecoder: Decoder[User] =
    deriveDecoder[User]
  implicit val ProjectEncoder: Encoder[User] =
    deriveEncoder[User]
}
