package taskforce.project

import cats.effect.kernel.Async
import cats.implicits._
import io.circe.refined._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import sttp.model.StatusCode
import sttp.tapir.{path, statusCode}
import sttp.tapir.json.circe._
import taskforce.authentication.Authenticator
import taskforce.common.ResponseError
import taskforce.common.ResponseError._
import taskforce.common.instances.{Http4s => CommonInstancesHttp4s}
import taskforce.project.ProjectName
import taskforce.project.TotalTime
import taskforce.common.DefaultEndpointInterpreter
import taskforce.common.BaseEndpoint

final class ProjectRoutes[F[_]: Async] private (
    authenticator: Authenticator[F],
    projectService: ProjectService[F]
) extends instances.Http4s[F]
    with CommonInstancesHttp4s[F]
    with instances.TapirCodecs
    with DefaultEndpointInterpreter
    with BaseEndpoint {

  private object endpoints {

    val base = endpoint.in("projects")

    val list =
      authenticator
        .secureEndpoints(base)
        .get
        .out(jsonBody[List[Project]].description("List of all created projects"))
        .description("Lists all created projects")
        .serverLogicSuccess(_ => _ => projectService.list)

    val find =
      authenticator
        .secureEndpoints(base)
        .get
        .in(path[ProjectId].description("Project ID"))
        .out(jsonBody[Project].description("Details of the selected project"))
        .description("Returns data for single project")
        .serverLogic { _ => projectId =>
          projectService
            .find(projectId)
            .map(Either.fromOption(_, ResponseError.NotFound("project not found")))
        }

    val create =
      authenticator
        .secureEndpoints(base)
        .post
        .in(jsonBody[ProjectName].description("Name of the created project"))
        .out(
          jsonBody[Project]
            .description("Details of the created project")
            .and(statusCode(StatusCode.Created))
        )
        .description("Creates new project")
        .serverLogic { userId => projectName =>
          projectService
            .create(projectName, userId)
            .map(_.leftMap(ResponseError.fromAppError))
            .extractFromEffectandMerge
        }

    val delete =
      authenticator
        .secureEndpoints(base)
        .delete
        .in(path[ProjectId].description("Project ID"))
        .out(statusCode(StatusCode.Ok))
        .description("Deletes project")
        .serverLogic { userId => projectId =>
          projectService.delete(projectId, userId).void.extractFromEffect
        }

    val totalTime =
      authenticator
        .secureEndpoints(base)
        .get
        .in(path[ProjectId].description("Project ID"))
        .in("totalTime")
        .out(jsonBody[TotalTime].description("Total time logged in a given project"))
        .description("Returns total time for given project")
        .serverLogic { _ => projectId =>
          projectService.totalTime(projectId).extractFromEffect
        }

    val update =
      authenticator
        .secureEndpoints(base)
        .put
        .in(path[ProjectId].description("Project ID"))
        .in(jsonBody[ProjectName].description("New project's name"))
        .out(jsonBody[Project].description("Details of the updated project"))
        .description("Renames existing project")
        .serverLogic { userId =>
          { case (projectId, projectName) =>
            projectService
              .update(projectId, projectName, userId)
              .map(_.leftMap(ResponseError.fromAppError))
              .extractFromEffectandMerge
          }
        }

    def routes: HttpRoutes[F] = toRoutes("projects")(find, list, totalTime, create, delete, update)

  }

  def routes: HttpRoutes[F] =
    Router("/" -> endpoints.routes)
}

object ProjectRoutes {
  def make[F[_]: Async](
      authenticator: Authenticator[F],
      projectService: ProjectService[F]
  ): ProjectRoutes[F] = new ProjectRoutes(authenticator, projectService)
}
