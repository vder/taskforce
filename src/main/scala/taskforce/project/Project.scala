package taskforce.project

import java.time.LocalDateTime
import taskforce.authentication.UserId
import io.circe.generic.JsonCodec
import io.circe.refined._



@JsonCodec final case class Project(
    id: ProjectId,
    name: ProjectName,
    author: UserId,
    created: LocalDateTime,
    deleted: Option[LocalDateTime]
)

