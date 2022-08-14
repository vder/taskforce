package taskforce.filter.model

import java.time.Instant
import java.util.UUID
import taskforce.task.Task
import taskforce.project.Project

final case class FilterResultRow(
    projectId: Long,
    projectName: String,
    projectCreated: Instant,
    projectDeleted: Option[Instant],
    projectAuthor: UUID,
    taskComment: Option[String],
    taskCreated: Option[Instant],
    taskDeleted: Option[Instant],
    duration: Option[Long]
)

object FilterResultRow {

  def fromTuple(x: (Project, Option[Task])): FilterResultRow =
    x match {
      case (p, tOpt) =>
        FilterResultRow(
          p.id.value,
          p.name.value.value,
          p.created.value,
          p.deleted.map(_.value),
          p.author.value,
          tOpt.flatMap(_.comment.map(_.value.value)),
          tOpt.map(_.created.value),
          tOpt.flatMap(_.deleted.map(_.value)),
          tOpt.map(_.duration.value.toMinutes)
        )
    }

}
