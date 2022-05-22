package taskforce.task

import cats.effect.Sync
import cats.implicits._
import io.circe.syntax._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRequest, AuthedRoutes}
import taskforce.common.{ErrorMessage, ErrorHandler, errors => commonErrors}
import taskforce.authentication.UserId
import org.http4s.Response

final class TaskRoutes[F[_]: Sync: JsonDecoder](
    authMiddleware: AuthMiddleware[F, UserId],
    taskService: TaskService[F]
)  {

  private[this] val prefixPath = "/api/v1/projects"

  val dsl = new Http4sDsl[F] {}
  import dsl._

  val prepareFailedResponse: PartialFunction[TaskError, F[Response[F]]] = {
    case DuplicateTaskNameError(name) =>
      Conflict(
        ErrorMessage("TASK-001", s"name given in request: $name already exists")
      )
    case WrongPeriodError =>
      Conflict(
        ErrorMessage("TASK-002", "Reporter already logged task in a given time")
      )
  }

  def getTaskFromAuthReq(
      authReq: AuthedRequest[F, UserId],
      userId: UserId,
      projectId: ProjectId
  ): F[Task] =
    authReq.req
      .asJsonDecode[NewTask]
      .adaptError(_ => commonErrors.BadRequest)
      .map(t => Task.fromNewTask(t, userId, projectId))

  val httpRoutes: AuthedRoutes[UserId, F] = {

    AuthedRoutes.of {
      case GET -> Root / LongVar(projectId) / "tasks" as _ =>
        val taskStream = taskService.list(ProjectId(projectId)).map(x => x.asJson)
        Ok(taskStream)
      case GET -> Root / LongVar(projectId) / "tasks" / UUIDVar(taskId)
          as _ =>
        taskService.find(ProjectId(projectId), TaskId(taskId)).flatMap(Ok(_))

      case authReq @ POST -> Root / LongVar(projectId) / "tasks" as userId =>
        for {
          task   <- getTaskFromAuthReq(authReq, userId, ProjectId(projectId))
          result <- taskService.create(task)
          response <- result match {
            case Left(err)   => prepareFailedResponse(err)
            case Right(task) => Created(task)
          }
        } yield response

      case authReq @ PUT -> Root / LongVar(
            projectId
          ) / "tasks" / UUIDVar(taskId) as userId =>
        for {
          task   <- getTaskFromAuthReq(authReq, userId, ProjectId(projectId))
          result <- taskService.update(TaskId(taskId), task, userId)
          response <- result match {
            case Left(err)   => prepareFailedResponse(err)
            case Right(task) => Ok(task)
          }
        } yield response

      case DELETE -> Root / LongVar(
            projectId
          ) / "tasks" / UUIDVar(taskId) as userId =>
        taskService.delete(ProjectId(projectId), TaskId(taskId), userId) *> Ok()
    }
  }

  def routes(errHandler: ErrorHandler[F, Throwable]) =
    Router(
      prefixPath -> errHandler.basicHandle(authMiddleware(httpRoutes))
    )
}

object TaskRoutes {
  def make[F[_]: Sync: JsonDecoder](
      authMiddleware: AuthMiddleware[F, UserId],
      taskService: TaskService[F]
  ) = Sync[F].delay { new TaskRoutes(authMiddleware, taskService) }
}
