package taskforce.task

import cats.effect.Sync
import cats.implicits._
import taskforce.common.AppError
import taskforce.authentication.UserId
import java.time.Instant

final class TaskService[F[_]: Sync] private (
    taskRepo: TaskRepository[F]
) {
  def list(projectId: ProjectId): fs2.Stream[F, Task] = taskRepo.list(projectId)

  def find(projectId: ProjectId, taskId: TaskId): F[Option[Task]] =
    taskRepo
      .find(projectId, taskId)

  def create(task: Task): F[Either[AppError, Task]] =
    for {
      allUserTasks <- taskRepo.listByUser(task.author).pure[F]
      _            <- taskPeriodIsValid(task, allUserTasks)
      result       <- taskRepo.create(task)
    } yield result.leftWiden[AppError]

  def update(
      taskId: TaskId,
      task: Task,
      caller: UserId
  ): F[Either[AppError, Task]] =
    for {
      oldTask      <- getTaskIfAuthor(task.projectId, taskId, caller)
      allUserTasks <- taskRepo.listByUser(task.author).pure[F]
      allUserTasksWithoutOld = allUserTasks.filterNot(_.id == oldTask.id)
      _           <- taskPeriodIsValid(task, allUserTasksWithoutOld)
      updatedTask <- taskRepo.update(oldTask.id, task)
    } yield updatedTask.leftWiden[AppError]

  def delete(projectId: ProjectId, taskId: TaskId, caller: UserId) =
    for {
      task   <- getTaskIfAuthor(projectId, taskId, caller)
      result <- taskRepo.delete(task.id)
    } yield result

  private def taskPeriodIsValid(
      newTask: Task,
      userTasks: fs2.Stream[F, Task]
  ): F[Boolean] = {
    val taskEnd: Instant =
      newTask.created.value.plus(newTask.duration.value)
    val taskStart: Instant = newTask.created.value
    for {
      isValid <-
        userTasks
          .collect {
            case t if t.deleted.isEmpty =>
              (t.created.value, t.created.value.plus(t.duration.value))
          }
          .forall { case (start, end) =>
            (start.isAfter(taskEnd) || taskStart.isAfter(end))
          }
          .compile
          .toList
          .ensure(AppError.WrongPeriodError)(_.head)
    } yield isValid.head
  }

  private def getTaskIfAuthor(
      projectId: ProjectId,
      taskId: TaskId,
      userId: UserId
  ) =
    for {
      taskOption <- taskRepo.find(projectId, taskId)
      task <-
        Sync[F]
          .fromOption(
            taskOption,
            AppError.NotFound(taskId.value.toString())
          )
          .ensure(AppError.NotAuthor(userId.value))(_.author == userId)
    } yield task

}

object TaskService {
  def make[F[_]: Sync](taskRepo: TaskRepository[F]): TaskService[F] =
    new TaskService[F](taskRepo)

}
