package taskforce.filter

import eu.timepit.refined.types.string.NonEmptyString
import java.time.LocalDateTime
import java.util.UUID
import taskforce.project.Project
import taskforce.task.Task

sealed trait Operator

final case object Eq   extends Operator
final case object Lt   extends Operator
final case object Gt   extends Operator
final case object Lteq extends Operator
final case object Gteq extends Operator

sealed trait Status

final case object Active   extends Status
final case object Deactive extends Status
final case object All      extends Status

sealed trait Criteria extends Product with Serializable

final case class In(names: List[NonEmptyString])                    extends Criteria
final case class TaskCreatedDate(op: Operator, date: LocalDateTime) extends Criteria
final case class State(status: Status)                              extends Criteria

final case class FilterId(value: UUID)
final case class NewFilter(conditions: List[Criteria])
final case class Filter(id: FilterId, conditions: List[Criteria])

case class FilterResultRow(
    projectId: Long,
    projectName: String,
    projectCreated: LocalDateTime,
    projectDeleted: Option[LocalDateTime],
    projectAuthor: UUID,
    taskComment: Option[String],
    taskCreated: Option[LocalDateTime],
    taskDeleted: Option[LocalDateTime],
    duration: Option[Long]
)

object FilterResultRow {

  def fromTuple(x: (Project, Option[Task])) =
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
