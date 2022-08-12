package taskforce.task

import cats.effect.kernel.Async
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import taskforce.authentication.Authenticator
import taskforce.common.BaseEndpoint
import taskforce.common.ResponseError
import taskforce.common.ResponseError._

import taskforce.task.ProjectId
import taskforce.common.DefaultEndpointInterpreter
import taskforce.common.StreamingResponse
import java.nio.charset.StandardCharsets
import sttp.capabilities.fs2.Fs2Streams



final class TaskRoutes[F[_]: Async] private (
    authenticator: Authenticator[F],
    taskService: TaskService[F]
) extends instances.Http4s[F]
    with instances.TapirCodecs
    with BaseEndpoint
    with DefaultEndpointInterpreter
    with StreamingResponse {
  private object endpoints {

    val base = endpoint.in("projects")

    val list =
      authenticator
        .secureEndpoint(base)
        .get
        .in(path[ProjectId].description("project ID"))
        .in("tasks")
        .out(streamBody(Fs2Streams[F])(Schema.binary, CodecFormat.Json(), Some(StandardCharsets.UTF_8)))
        .serverLogicSuccess(_ =>
          projectId =>
            taskService
              .list(projectId)
              .through(wrapInArray)
              .pure[F]
        )

    val find =
      authenticator
        .secureEndpoint(base)
        .get
        .in(path[ProjectId].description("project ID"))
        .in("tasks")
        .in(path[TaskId].description("task ID"))
        .out(jsonBody[Task].description("returns task with given ID"))
        .description("Returns task with a given Id")
        .serverLogic { _ =>
          { case (projectId, taskId) =>
            taskService
              .find(projectId, taskId)
              .map(Either.fromOption(_, ResponseError.NotFound(s"resource $taskId is not found")))
          }
        }

    val create =
      authenticator
        .secureEndpoint(base)
        .post
        .in(path[ProjectId].description("project ID"))
        .in("tasks")
        .in(jsonBody[NewTask].description("specifies a new task"))
        .out(jsonBody[Task].and(statusCode(StatusCode.Created)))
        .description("Create a new task in a project")
        .serverLogic { userId =>
          { case (projectId, newTask) =>
            taskService
              .create(Task.fromNewTask(newTask, userId, projectId))
              .map(_.leftMap(ResponseError.fromAppError))
              .extractFromEffectandMerge
          }
        }

    val delete =
      authenticator
        .secureEndpoint(base)
        .delete
        .in(path[ProjectId].description("project ID"))
        .in("tasks")
        .in(path[TaskId].description("task ID"))
        .out(statusCode(StatusCode.Ok))
        .description("Deletes a task in a project")
        .serverLogic { userId =>
          { case (projectId, taskId) =>
            taskService
              .delete(projectId, taskId, userId)
              .void
              .extractFromEffect
          }
        }

    val update =
      authenticator
        .secureEndpoint(base)
        .put
        .in(path[ProjectId].description("project ID"))
        .in("tasks")
        .in(path[TaskId].description("task ID"))
        .in(jsonBody[NewTask].description("specifies a new task"))
        .out(jsonBody[Task].description("returns created task"))
        .description("Updates a task in a project")
        .serverLogic { userId =>
          { case (projectId, taskId, newTask) =>
            val task = Task.fromNewTask(newTask, userId, projectId)
            taskService
              .update(taskId, task, userId)
              .map(_.leftMap(ResponseError.fromAppError))
              .extractFromEffectandMerge
          }
        }

    def routes: HttpRoutes[F] = toRoutes("tasks")(find, list, create, delete, update)

  }

  def routes =
    Router(
      "/" -> endpoints.routes
    )
}

object TaskRoutes {
  def make[F[_]: Async](
      authenticator: Authenticator[F],
      taskService: TaskService[F]
  ) = new TaskRoutes(authenticator, taskService)
}
