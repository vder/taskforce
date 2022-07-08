package taskforce.project

import taskforce.authentication.UserId

import taskforce.common.CreationDate
import taskforce.common.DeletionDate
import io.circe.refined._
import io.circe.generic.JsonCodec

@JsonCodec final case class Project(
    id: ProjectId,
    name: ProjectName,
    author: UserId,
    created: CreationDate,
    deleted: Option[DeletionDate]
)
