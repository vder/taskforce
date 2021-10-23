package taskforce.authentication

import java.util.UUID

final case class UserId(value: UUID) extends AnyVal
final case class User(id: UserId)
