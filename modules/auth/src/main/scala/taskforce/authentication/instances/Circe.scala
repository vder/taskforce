package taskforce.authentication.instances

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import taskforce.authentication.User
import monix.newtypes.integrations.DerivedCirceCodec
trait Circe extends DerivedCirceCodec {

  implicit val userDecoder: Decoder[User] =
    deriveDecoder[User]
  implicit val userEncoder: Encoder[User] =
    deriveEncoder[User]

}
