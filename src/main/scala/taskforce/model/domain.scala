package taskforce.model

import java.util.UUID
import doobie.util.meta.Meta
import io.circe.Decoder
import io.circe.Encoder
import io.circe.refined._
import io.circe.generic.semiauto._
import eu.timepit.refined.types.string.NonEmptyString
import java.time.LocalDateTime

object domain {
  final case class UserId(id: UUID) extends AnyVal

  object UserId {
    implicit val userIdDecoder: Decoder[UserId] = deriveDecoder[UserId]
    implicit val userIdEncoder: Encoder[UserId] = deriveEncoder[UserId]
  }

  final case class NewProject(name: NonEmptyString)

  final case class Project(
      id: Long,
      name: NonEmptyString,
      owner: UserId,
      created: LocalDateTime,
      deleted: Option[LocalDateTime]
  )

  object Project {
    implicit val ProjectDecoder: Decoder[Project] =
      deriveDecoder[Project]
    implicit val ProjectEncoder: Encoder[Project] =
      deriveEncoder[Project]
  }

  object NewProject {
    implicit val NewProjectDecoder: Decoder[NewProject] =
      deriveDecoder[NewProject]
    implicit val NewProjectEncoder: Encoder[NewProject] =
      deriveEncoder[NewProject]
  }
}
