package taskforce.task

import cats.effect.kernel.Async
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.json.circe._
import taskforce.authentication.Authenticator
import taskforce.common.BaseApi
import taskforce.common.ResponseError
import taskforce.common.ResponseError._
import taskforce.common.instances.{Http4s => CommonInstancesHttp4s}

import java.util.UUID
import taskforce.common.DefaultEndpointInterpreter

final class TaskRoutes[F[_]: Async] private (
    authenticator: Authenticator[F],
    taskService: TaskService[F]
) extends instances.Http4s[F]
    with CommonInstancesHttp4s[F]
    with instances.TapirCodecs
    with DefaultEndpointInterpreter {
  private object endpoints {

    val base = BaseApi.endpoint.in("projects")

    val list =
      authenticator
        .secureEndpoints(base)
        .get
        .in(path[Long].description("project ID"))
        .in("tasks")
        .out(jsonBody[List[Task]].description("tasks in given project"))
        .description("Lists all task in a given project")
        .serverLogicSuccess(_ => projectId => taskService.list(ProjectId(projectId)).compile.toList)

    val find =
      authenticator
        .secureEndpoints(base)
        .get
        .in(path[Long].description("project ID"))
        .in("tasks")
        .in(path[UUID].description("task ID"))
        .out(jsonBody[Task].description("returns task with given ID"))
        .description("Returns task with a given Id")
        .serverLogic { _ =>
          { case (projectId, taskId) =>
            taskService
              .find(ProjectId(projectId), TaskId(taskId))
              .map(Either.fromOption(_, ResponseError.NotFound(s"resource $taskId is not found")))
          }
        }

    val create =
      authenticator
        .secureEndpoints(base)
        .post
        .in(path[Long].description("project ID"))
        .in("tasks")
        .in(jsonBody[NewTask].description("specifies a new task"))
        .out(jsonBody[Task].and(statusCode(StatusCode.Created)))
        .description("Create a new task in a project")
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
        .secureEndpoints(base)
        .delete
        .in(path[Long].description("project ID"))
        .in("tasks")
        .in(path[UUID].description("task ID"))
        .out(statusCode(StatusCode.Ok))
        .description("Deletes a task in a project")
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
        .secureEndpoints(base)
        .put
        .in(path[Long].description("project ID"))
        .in("tasks")
        .in(path[UUID].description("task ID"))
        .in(jsonBody[NewTask].description("specifies a new task"))
        .out(jsonBody[Task].description("returns created task"))
        .description("Updates a task in a project")
        .serverLogic { userId =>
          { case (projectId, taskId, newTask) =>
            val task = Task.fromNewTask(newTask, userId, ProjectId(projectId))
            taskService
              .update(TaskId(taskId), task, userId)
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
