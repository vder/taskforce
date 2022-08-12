package taskforce.task

import taskforce.authentication.UserId
import taskforce.common._
import taskforce.task.TaskVolume
import taskforce.task.TaskComment

import cats.effect.std.UUIDGen
import cats.effect.kernel.Clock
import cats.Monad
import cats.implicits._

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

  def fromNewTask[F[_]: Monad: Clock: UUIDGen](
      newTask: NewTask,
      userId: UserId,
      projectId: ProjectId
  ): F[Task] =
    for {
      taskId       <- UUIDGen[F].randomUUID.map(TaskId(_))
      creationDate <- newTask.created.fold(Clock[F].realTimeInstant.map(CreationDate(_)))(_.pure[F])
    } yield Task(
      taskId,
      projectId,
      userId,
      creationDate,
      newTask.duration,
      newTask.volume,
      None,
      newTask.comment
    )

}
