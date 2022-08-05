package taskforce.project

import cats.implicits._
import taskforce.common.instances.{Http4s => CommonInstancesHttp4s}
import io.circe.refined._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerInterpreter
import cats.effect.kernel.Async
import sttp.tapir._
import sttp.tapir.json.circe._
import taskforce.common.ResponseError
import taskforce.common.ResponseError._
import sttp.model.StatusCode
import taskforce.authentication.Authenticator
import taskforce.project.TotalTime
import taskforce.project.ProjectName
import org.http4s.server.Router
import taskforce.common.BaseApi


final class ProjectRoutes[F[_]: Async] private (
    authenticator: Authenticator[F],
    projectService: ProjectService[F]
) extends instances.Http4s[F] with CommonInstancesHttp4s[F] with instances.TapirCodecs{

  
  object endpoints {

    val list =
      authenticator.secureEndpoints(BaseApi.endpoint).get
        .out(jsonBody[List[Project]])
        .serverLogicSuccess(_ => _ => projectService.list)

    val find =
      authenticator.secureEndpoints(BaseApi.endpoint).get
        .in(path[Long])
        .out(jsonBody[Project])
        .serverLogic { _ => projectId =>
          projectService
            .find(ProjectId(projectId))
            .map(Either.fromOption(_, ResponseError.NotFound("project not found")))
        }

    val create =
      authenticator.secureEndpoints(BaseApi.endpoint).post
        .in(jsonBody[ProjectName])
        .out(jsonBody[Project].and(statusCode(StatusCode.Created)))
        .serverLogic { userId => projectName =>
          projectService
            .create(projectName, userId)
            .map(_.leftMap(ResponseError.fromAppError))
            .extractFromEffectandMerge
        }

    val delete =
      authenticator.secureEndpoints(BaseApi.endpoint).delete
        .in(path[Long])
        .out(statusCode(StatusCode.Ok))
        .serverLogic { userId => projectId =>
          projectService.delete(ProjectId(projectId), userId).void.extractFromEffect
        }

    val totalTime =
      authenticator.secureEndpoints(BaseApi.endpoint).get
        .in(path[Long])
        .in("totalTime")
        .out(jsonBody[TotalTime])
        .serverLogic { _ => projectId =>
          projectService.totalTime(ProjectId(projectId)).extractFromEffect
        }

    val update =
      authenticator.secureEndpoints(BaseApi.endpoint).put
        .in(path[Long])
        .in(jsonBody[ProjectName])
        .out(jsonBody[Project])
        .serverLogic { userId =>
          { case (projectId, projectName) =>
            projectService
              .update(ProjectId(projectId), projectName, userId)
              .map(_.leftMap(ResponseError.fromAppError))
              .extractFromEffectandMerge
          }
        }

    def routes: HttpRoutes[F] =
      (find :: list :: totalTime :: create :: delete :: update :: Nil)
        .map(Http4sServerInterpreter[F]().toRoutes(_))
        .reduce(_ <+> _)

  }

  private[this] val prefixPath = "/api/v1/projects"

  def routes: HttpRoutes[F] =
    Router(
      prefixPath -> endpoints.routes
    )
}

object ProjectRoutes {
  def make[F[_]: Async](
      authenticator: Authenticator[F],
      projectService: ProjectService[F]
  ): ProjectRoutes[F] = new ProjectRoutes(authenticator, projectService)
}
