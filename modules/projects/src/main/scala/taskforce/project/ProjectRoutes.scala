package taskforce.project

import cats.implicits._
import io.circe.refined._
import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerInterpreter
import cats.effect.kernel.Async
import io.circe.generic.auto._
import sttp.tapir.generic.auto._
import sttp.tapir._
import sttp.tapir.json.circe._
import taskforce.common.ResponseError
import taskforce.common.ResponseError._
import sttp.model.StatusCode
import taskforce.authentication.Authenticator
import taskforce.project.TotalTime
import taskforce.project.ProjectName
import org.http4s.server.Router

final class ProjectRoutes[F[_]: Async] private (
    authenticator: Authenticator[F],
    projectService: ProjectService[F]
) extends instances.Http4s[F]
    with instances.TapirCodecs {

  val baseEndpoint: Endpoint[String, Unit, ResponseError, Unit, Any] =
    endpoint
      .securityIn(auth.bearer[String]())
      .errorOut(
        oneOf[ResponseError](
          oneOfVariant[TokenDecoding](
            statusCode(StatusCode.Unauthorized)
              .and(jsonBody[TokenDecoding].description("invalid token"))
          ),
          oneOfVariant[Forbidden](
            statusCode(StatusCode.Unauthorized)
              .and(jsonBody[Forbidden].description("Unknown User"))
          ),
          oneOfVariant[NotFound](
            statusCode(StatusCode.NotFound)
              .and(jsonBody[NotFound].description("resource not found"))
          ),
          oneOfVariant[NotAuthor](
            statusCode(StatusCode.Forbidden)
              .and(jsonBody[NotAuthor].description("authorised user is not owner of the resource"))
          ),
          oneOfVariant[DuplicateProjectName2](
            jsonBody[DuplicateProjectName2]
              .description("project's name is already in use")
              .and(statusCode(StatusCode.Conflict))
          )
        )
      )

  object endpoints {

    val list =
      authenticator.secureEndpoints(baseEndpoint).get
        .out(jsonBody[List[Project]])
        .serverLogicSuccess(_ => _ => projectService.list)

    val find =
      authenticator.secureEndpoints(baseEndpoint).get
        .in(path[Long])
        .out(jsonBody[Project])
        .serverLogic { _ => projectId =>
          projectService
            .find(ProjectId(projectId))
            .map(Either.fromOption(_, ResponseError.NotFound("project not found")))
        }

    val create =
      authenticator.secureEndpoints(baseEndpoint).post
        .in(jsonBody[ProjectName])
        .out(jsonBody[Project].and(statusCode(StatusCode.Created)))
        .serverLogic { userId => projectName =>
          projectService
            .create(projectName, userId)
            .map(_.leftMap(ResponseError.fromAppError))
            .extractFromEffectandMerge
        }

    val delete =
      authenticator.secureEndpoints(baseEndpoint).delete
        .in(path[Long])
        .out(statusCode(StatusCode.Ok))
        .serverLogic { userId => projectId =>
          projectService.delete(ProjectId(projectId), userId).void.extractFromEffect
        }

    val totalTime =
      authenticator.secureEndpoints(baseEndpoint).get
        .in(path[Long])
        .in("totalTime")
        .out(jsonBody[TotalTime])
        .serverLogic { _ => projectId =>
          projectService.totalTime(ProjectId(projectId)).extractFromEffect
        }

    val update =
      authenticator.secureEndpoints(baseEndpoint).put
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
