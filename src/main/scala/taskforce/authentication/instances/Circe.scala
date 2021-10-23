package taskforce.authentication.instances

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import java.util.UUID
import taskforce.authentication.{User, UserId}
trait Circe {

  implicit val userIdDecoder: Decoder[UserId] =
    Decoder[UUID].map(UserId.apply)
  implicit val userIdEncoder: Encoder[UserId] =
    Encoder[UUID].contramap(_.value)

  implicit val userDecoder: Decoder[User] =
    deriveDecoder[User]
  implicit val userEncoder: Encoder[User] =
    deriveEncoder[User]

}
