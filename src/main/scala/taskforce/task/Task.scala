package taskforce.task

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.string.NonEmptyString
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import taskforce.project.ProjectId
import taskforce.authentication.UserId
import taskforce.common.CreationDate
import taskforce.common.DeletionDate

final case class TaskId(value: UUID) extends AnyVal

final case class NewTask(
    created: Option[CreationDate] = None,
    duration: TaskDuration,
    volume: Option[Int Refined Positive],
    comment: Option[NonEmptyString]
)

final case class TaskDuration(value: Duration) extends AnyVal

final case class Task(
    id: TaskId,
    projectId: ProjectId,
    author: UserId,
    created: CreationDate,
    duration: TaskDuration,
    volume: Option[Int Refined Positive],
    deleted: Option[DeletionDate],
    comment: Option[NonEmptyString]
)

object Task {

  def fromNewTask(
      newTask: NewTask,
      userId: UserId,
      projectId: ProjectId
  ) =
    Task(
      TaskId(UUID.randomUUID()),
      projectId,
      userId,
      newTask.created.fold(CreationDate(LocalDateTime.now()))(identity),
      newTask.duration,
      newTask.volume,
      None,
      newTask.comment
    )

}
