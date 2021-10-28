package taskforce.task

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.string.NonEmptyString
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import taskforce.project.ProjectId
import taskforce.authentication.UserId

final case class TaskId(value: UUID) extends AnyVal

final case class NewTask(
    created: Option[LocalDateTime],
    duration: TaskDuration,
    volume: Option[Int Refined Positive],
    comment: Option[NonEmptyString]
)

final case class TaskDuration(value: Duration) extends AnyVal

final case class Task(
    id: TaskId,
    projectId: ProjectId,
    author: UserId,
    created: LocalDateTime,
    duration: TaskDuration,
    volume: Option[Int Refined Positive],
    deleted: Option[LocalDateTime],
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
      newTask.created.getOrElse(LocalDateTime.now()),
      newTask.duration,
      newTask.volume,
      None,
      newTask.comment
    )

}
