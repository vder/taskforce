package taskforce.task

import cats.effect.Sync
import cats.implicits._
import taskforce.common.{errors => commonErrors}
import taskforce.authentication.UserId
import taskforce.project.ProjectId

final class TaskService[F[_]: Sync](
    taskRepo: TaskRepository[F]
) {

  private def taskPeriodIsValid(
      newTask: Task,
      userTasks: fs2.Stream[F, Task]
  ): F[Boolean] = {
    val taskEnd   = newTask.created.plus(newTask.duration.value)
    val taskStart = newTask.created
    for {
      isValid <-
        userTasks
          .collect {
            case t if t.deleted.isEmpty =>
              (t.created, t.created.plus(t.duration.value))
          }
          .forall { case (start, end) =>
            (start.isAfter(taskEnd) || taskStart.isAfter(end))
          }
          .compile
          .toList
          .ensure(WrongPeriodError)(_.head)
    } yield isValid.head
  }

  private def getTaskIfAuthor(projectId: ProjectId, taskId: TaskId, userId: UserId) =
    for {
      taskOption <- taskRepo.find(projectId, taskId)
      task <-
        Sync[F]
          .fromOption(
            taskOption,
            commonErrors.NotFound(taskId.value.toString())
          )
          .ensure(commonErrors.NotAuthor(userId))(_.author == userId)
    } yield task

  def list(projectId: ProjectId) = taskRepo.list(projectId)

  def find(projectId: ProjectId, taskId: TaskId) =
    taskRepo.find(projectId, taskId).ensure(commonErrors.NotFound(taskId.value.toString))(_.isDefined)

  def create(task: Task): F[Either[TaskError, Task]] =
    (for {
      allUserTasks <- Sync[F].delay(taskRepo.listByUser(task.author))
      _            <- taskPeriodIsValid(task, allUserTasks)
      result       <- taskRepo.create(task)
    } yield result.leftWiden[TaskError]).recover { case WrongPeriodError => WrongPeriodError.asLeft[Task] }

  def update(taskId: TaskId, task: Task, caller: UserId): F[Either[TaskError, Task]] =
    (for {
      oldTask      <- getTaskIfAuthor(task.projectId, taskId, caller)
      allUserTasks <- Sync[F].delay(taskRepo.listByUser(task.author))
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
    Sync[F].delay(
      new TaskService[F](taskRepo)
    )
}
