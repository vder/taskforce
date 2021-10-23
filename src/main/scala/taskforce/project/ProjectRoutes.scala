package taskforce.project

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, MonadError}
import org.http4s.{AuthedRequest, AuthedRoutes, Response}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import taskforce.authentication.UserId
import taskforce.common.{ErrorMessage, ErrorHandler, errors => commonErrors}

final class ProjectRoutes[F[_]: Sync: Applicative: MonadError[*[_], Throwable]: JsonDecoder](
    authMiddleware: AuthMiddleware[F, UserId],
    projectService: ProjectService[F]
) extends instances.Http4s[F] {

  val dsl = new Http4sDsl[F] {}
  import dsl._

  private[this] val prefixPath = "/api/v1/projects"

  val prepareFailedResponse: PartialFunction[ProjectError, F[Response[F]]] = {
    case DuplicateProjectNameError(newProject) =>
      Conflict(
        ErrorMessage("PROJECT-001", s"name given in request: ${newProject.name} already exists")
      )
  }

  def newProjectFromReq(authReq: AuthedRequest[F, UserId]) =
    authReq.req
      .asJsonDecode[NewProject]
      .adaptError(_ => commonErrors.BadRequest)

  val httpRoutes: AuthedRoutes[UserId, F] = {

    AuthedRoutes.of {
      case GET -> Root as userId =>
        projectService.list.flatMap(Ok(_))

      case DELETE -> Root / LongVar(projectId) as userId =>
        projectService.delete(ProjectId(projectId), userId) *> Ok()

      case GET -> Root / LongVar(projectId) as userId =>
        projectService.find(ProjectId(projectId)).flatMap(Ok(_))

      case GET -> Root / LongVar(projectId) / "totalTime" as userId =>
        projectService.totalTime(ProjectId(projectId)).flatMap(Ok(_))

      case authReq @ POST -> Root as userId =>
        for {
          newProject    <- newProjectFromReq(authReq)
          projectResult <- projectService.create(newProject, userId)
          response <- projectResult match {
            case Left(err)      => prepareFailedResponse(err)
            case Right(project) => Created(project)
          }
        } yield response

      case authReq @ PUT -> Root / IntVar(projectId) as userId =>
        for {
          newProject    <- newProjectFromReq(authReq)
          updatedResult <- projectService.update(ProjectId(projectId), newProject, userId)
          response <- updatedResult match {
            case Left(err)      => prepareFailedResponse(err)
            case Right(project) => Ok(project)
          }
        } yield response

    }
  }

  def routes(errHandler: ErrorHandler[F, Throwable]) =
    Router(
      prefixPath -> errHandler.basicHandle(authMiddleware(httpRoutes))
    )
}

object ProjectRoutes {
  def make[F[_]: MonadError[*[_], Throwable]: Sync: JsonDecoder](
      authMiddleware: AuthMiddleware[F, UserId],
      projectService: ProjectService[F]
  ) = Sync[F].delay { new ProjectRoutes(authMiddleware, projectService) }
}
