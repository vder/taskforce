package taskforce.authentication

import io.circe.generic.JsonCodec

@JsonCodec final case class User(id: UserId)
