package taskforce.authentication.instances

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import taskforce.authentication.User
import monix.newtypes.integrations.DerivedCirceCodec
trait Circe extends DerivedCirceCodec{

  // implicit val userIdDecoder: Decoder[UserId] =
  //   Decoder[UUID].map(UserId.apply)
  // implicit val userIdEncoder: Encoder[UserId] =
  //   Encoder[UUID].contramap(_.value)

  implicit val userDecoder: Decoder[User] =
    deriveDecoder[User]
  implicit val userEncoder: Encoder[User] =
    deriveEncoder[User]

}
