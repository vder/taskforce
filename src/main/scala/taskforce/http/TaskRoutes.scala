package taskforce.http

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Defer, MonadError}
import io.circe.syntax._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRequest, AuthedRoutes}
import taskforce.Validations
import taskforce.model._
import taskforce.model.errors._
import taskforce.repository.{ProjectRepository, TaskRepository}

final class TaskRoutes[
    F[_]: Sync: Applicative: MonadError[
      *[_],
      Throwable
    ]: JsonDecoder
](
    authMiddleware: AuthMiddleware[F, UserId],
    projectRepo: ProjectRepository[F],
    taskRepo: TaskRepository[F]
) {

  private[this] val prefixPath = "/api/v1/projects"

  implicit def decodeTask = jsonOf
  implicit def encodeTask = jsonEncoderOf

  def getTaskFromAuthReq(
      authReq: AuthedRequest[F, UserId],
      userId: UserId,
      projectId: ProjectId
  ): F[Task] =
    authReq.req
      .asJsonDecode[NewTask]
      .adaptError(_ => BadRequestError)
      .map(t => Task.fromNewTask(t, userId, projectId))

  def getTaskIfOwner(projectId: ProjectId, taskId: TaskId, userId: UserId) =
    for {
      taskOption <- taskRepo.getTask(projectId, taskId)
      task <-
        Sync[F]
          .fromOption(
            taskOption,
            InvalidTask(projectId, taskId)
          )
          .ensure(NotAuthorError(userId))(_.owner == userId)
    } yield task

  val httpRoutes: AuthedRoutes[UserId, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    AuthedRoutes.of {
      case GET -> Root / IntVar(projectId) / "tasks" as userId =>
        val taskStream = taskRepo.getAllTasks(projectId).map(x => x.asJson)
        Ok(taskStream)
      case GET -> Root / IntVar(projectId) / "tasks" / UUIDVar(taskId)
          as userId =>
        for {
          task <- taskRepo.getTask(ProjectId(projectId), TaskId(taskId))
          response <- Ok(task.asJson)
        } yield response
      case authReq @ POST -> Root / IntVar(projectId) / "tasks" as userId =>
        for {
          task <- getTaskFromAuthReq(authReq, userId, ProjectId(projectId))
          allUserTasks <- Sync[F].delay(taskRepo.getAllUserTasks(task.owner))
          _ <- Validations.taskPeriodIsValid(task, allUserTasks)
          _ <- taskRepo.createTask(task)
          response <- Created(task)
        } yield response
      case authReq @ PUT -> Root / IntVar(
            projectId
          ) / "tasks" / UUIDVar(taskId) as userId =>
        for {
          task <- getTaskFromAuthReq(authReq, userId, ProjectId(projectId))
          oldTask <-
            getTaskIfOwner(ProjectId(projectId), TaskId(taskId), userId)
          allUserTasks <- Sync[F].delay(taskRepo.getAllUserTasks(task.owner))
          allUserTasksWithoutOld = allUserTasks.filterNot(_.id == oldTask.id)
          _ <- Validations.taskPeriodIsValid(task, allUserTasksWithoutOld)
          _ <- taskRepo.updateTask(oldTask.id, task)
          response <- Created(task)
        } yield response
      case authReq @ DELETE -> Root / IntVar(
            projectId
          ) / "tasks" / UUIDVar(taskId) as userId =>
        for {
          task <- getTaskIfOwner(ProjectId(projectId), TaskId(taskId), userId)
          _ <- taskRepo.deleteTask(task.id)
          response <- Created(task)
        } yield response
    }
  }

  val routes = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}

object TaskRoutes {
  def make[F[_]: Defer: MonadError[*[_], Throwable]: Sync](
      authMiddleware: AuthMiddleware[F, UserId],
      projectRepo: ProjectRepository[F],
      taskRepo: TaskRepository[F]
  ) = Sync[F].delay { new TaskRoutes(authMiddleware, projectRepo, taskRepo) }
}
