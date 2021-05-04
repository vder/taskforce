package taskforce.model

import doobie.util.meta.Meta
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.refined._
import java.time.LocalDateTime

final case class NewProject(name: NonEmptyString)

final case class ProjectId(value: Long) extends ResourceId[Long]

object ProjectId {
  implicit val taskIdDecoder: Decoder[ProjectId] =
    Decoder[Long].map(ProjectId.apply)
  implicit val taskIdEncoder: Encoder[ProjectId] =
    Encoder[Long].contramap(_.value)
  implicit val taskDurationMeta: Meta[ProjectId] =
    Meta[Long].imap(ProjectId.apply)(_.value)
}

final case class Project(
    id: ProjectId,
    name: NonEmptyString,
    author: UserId,
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
