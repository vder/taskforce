package taskforce.task

import cats.effect.Sync
import cats.implicits._
import taskforce.common.AppError
import taskforce.authentication.UserId
import java.time.Instant

final class TaskService[F[_]: Sync] private (
    taskRepo: TaskRepository[F]
) {

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
          .ensure(WrongPeriodError)(_.head)
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

  def list(projectId: ProjectId) = taskRepo.list(projectId)

  def find(projectId: ProjectId, taskId: TaskId) =
    taskRepo
      .find(projectId, taskId)
      .ensure(AppError.NotFound(taskId.value.toString))(_.isDefined)

  def create(task: Task): F[Either[TaskError, Task]] =
    (for {
      allUserTasks <- taskRepo.listByUser(task.author).pure[F]
      _            <- taskPeriodIsValid(task, allUserTasks)
      result       <- taskRepo.create(task)
    } yield result.leftWiden[TaskError]).recover { case WrongPeriodError =>
      WrongPeriodError.asLeft[Task]
    }

  def update(
      taskId: TaskId,
      task: Task,
      caller: UserId
  ): F[Either[TaskError, Task]] =
    (for {
      oldTask      <- getTaskIfAuthor(task.projectId, taskId, caller)
      allUserTasks <- taskRepo.listByUser(task.author).pure[F]
      allUserTasksWithoutOld = allUserTasks.filterNot(_.id == oldTask.id)
      _           <- taskPeriodIsValid(task, allUserTasksWithoutOld)
      updatedTask <- taskRepo.update(oldTask.id, task)
    } yield updatedTask.leftWiden[TaskError]).recover { case WrongPeriodError => WrongPeriodError.asLeft[Task] }

  def delete(projectId: ProjectId, taskId: TaskId, caller: UserId) =
    for {
      task   <- getTaskIfAuthor(projectId, taskId, caller)
      result <- taskRepo.delete(task.id)
    } yield result

}

object TaskService {
  def make[F[_]: Sync](taskRepo: TaskRepository[F]) =
    new TaskService[F](taskRepo)

}
