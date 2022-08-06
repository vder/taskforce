package taskforce.task

import cats.effect.kernel.Async
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import taskforce.authentication.Authenticator
import taskforce.common.BaseApi
import taskforce.common.ResponseError
import taskforce.common.ResponseError._
import taskforce.common.instances.{Http4s => CommonInstancesHttp4s}

import java.util.UUID

final class TaskRoutes[F[_]: Async] private (
    authenticator: Authenticator[F],
    taskService: TaskService[F]
) extends instances.Http4s[F]
    with CommonInstancesHttp4s[F]
    with instances.TapirCodecs {

  private[this] val prefixPath = "/api/v1/projects"

  object endpoints {

    val list =
      authenticator
        .secureEndpoints(BaseApi.endpoint)
        .get
        .in(path[Long])
        .in("tasks")
        .out(jsonBody[List[Task]])
        .serverLogicSuccess(_ => projectId => taskService.list(ProjectId(projectId)).compile.toList)

    val find =
      authenticator
        .secureEndpoints(BaseApi.endpoint)
        .get
        .in(path[Long])
        .in("tasks")
        .in(path[UUID])
        .out(jsonBody[Task])
        .serverLogic { _ =>
          { case (projectId, taskId) =>
            taskService
              .find(ProjectId(projectId), TaskId(taskId))
              .map(Either.fromOption(_, ResponseError.NotFound(s"resource $taskId is not found")))
          }
        }

    val create =
      authenticator
        .secureEndpoints(BaseApi.endpoint)
        .post
        .in(path[Long])
        .in("tasks")
        .in(jsonBody[NewTask])
        .out(jsonBody[Task].and(statusCode(StatusCode.Created)))
        .serverLogic { userId =>
          { case (projectId, newTask) =>
            taskService
              .create(Task.fromNewTask(newTask, userId, ProjectId(projectId)))
              .map(_.leftMap(ResponseError.fromAppError))
              .extractFromEffectandMerge
          }

        }

    val delete =
      authenticator
        .secureEndpoints(BaseApi.endpoint)
        .delete
        .in(path[Long])
        .in("tasks")
        .in(path[UUID])
        .out(statusCode(StatusCode.Ok))
        .serverLogic { userId =>
          { case (projectId, taskId) =>
            taskService
              .delete(ProjectId(projectId), TaskId(taskId), userId)
              .void
              .extractFromEffect
          }
        }

    val update =
      authenticator
        .secureEndpoints(BaseApi.endpoint)
        .put
        .in(path[Long])
        .in("tasks")
        .in(path[UUID])
        .in(jsonBody[NewTask])
        .out(jsonBody[Task])
        .serverLogic { userId =>
          { case (projectId, taskId, newTask) =>
            val task = Task.fromNewTask(newTask, userId, ProjectId(projectId))
            taskService
              .update(TaskId(taskId), task, userId)
              .map(_.leftMap(ResponseError.fromAppError))
              .extractFromEffectandMerge
          }
        }

    def routes: HttpRoutes[F] =
      (find :: list :: create :: delete :: update :: Nil)
        .map(Http4sServerInterpreter[F]().toRoutes(_))
        .reduce(_ <+> _)
  }

  def routes =
    Router(
      prefixPath -> endpoints.routes
    )
}

object TaskRoutes {
  def make[F[_]: Async](
      authenticator: Authenticator[F],
      taskService: TaskService[F]
  ) = new TaskRoutes(authenticator, taskService)
}
