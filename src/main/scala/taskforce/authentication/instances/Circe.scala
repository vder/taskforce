package taskforce.authentication.instances

import io.circe.{Decoder, Encoder}
import taskforce.authentication._

trait Circe {

  implicit val userDecoder: Decoder[User] =
    Decoder.forProduct1("id")(User.apply)
  implicit val userEncoder: Encoder[User] =
    Encoder.forProduct1("id")(_.id)

}
