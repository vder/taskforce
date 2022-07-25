package taskforce.project


import taskforce.authentication.UserId

import taskforce.common.CreationDate
import taskforce.common.DeletionDate


final case class Project(
    id: ProjectId,
    name: ProjectName,
    author: UserId,
    created: CreationDate,
    deleted: Option[DeletionDate]
)
