package taskforce.task

import java.time.LocalDateTime
import java.util.UUID
import taskforce.authentication.UserId
import taskforce.common._
import taskforce.task.TaskVolume
import taskforce.task.TaskComment

final case class NewTask(
    created: Option[CreationDate] = None,
    duration: TaskDuration,
    volume: Option[TaskVolume],
    comment: Option[TaskComment]
)

final case class Task(
    id: TaskId,
    projectId: ProjectId,
    author: UserId,
    created: CreationDate,
    duration: TaskDuration,
    volume: Option[TaskVolume],
    deleted: Option[DeletionDate],
    comment: Option[TaskComment]
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
