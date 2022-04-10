package taskforce.project

import taskforce.authentication.UserId
import io.circe.generic.JsonCodec
import io.circe.refined._
import taskforce.common.CreationDate
import taskforce.common.DeletionDate



@JsonCodec final case class Project(
    id: ProjectId,
    name: ProjectName,
    author: UserId,
    created: CreationDate,
    deleted: Option[DeletionDate]
)

