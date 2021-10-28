package taskforce.project

import eu.timepit.refined.types.string.NonEmptyString
import java.time.Duration
import java.time.LocalDateTime
import taskforce.authentication.UserId


final case class NewProject(name: NonEmptyString)

final case class ProjectId(value: Long) extends AnyVal
final case class Project(
    id: ProjectId,
    name: NonEmptyString,
    author: UserId,
    created: LocalDateTime,
    deleted: Option[LocalDateTime]
)

final case class TotalTime(value: Duration) extends AnyVal
