package taskforce.http

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Defer, Monad, MonadError}
import dev.profunktor.auth.JwtAuthMiddleware
import io.circe.syntax._
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.postgresql.util.PSQLException
import taskforce.model.domain._
import taskforce.model.errors._
import taskforce.repository.ProjectRepository

final class ProjectRoutes[
    F[_]: Defer: Applicative: MonadError[*[_], Throwable]: JsonDecoder
](
    authMiddleware: AuthMiddleware[F, UserId],
    projectRepo: ProjectRepository[F]
) {

  private[this] val prefixPath = "/api/v1/projects"

  val httpRoutes: AuthedRoutes[UserId, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    AuthedRoutes.of {
      case GET -> Root as userId =>
        for {
          projectList <- projectRepo.getAllProject
          response <- Ok(projectList.asJson)
        } yield response
      case DELETE -> Root / IntVar(projectId) as userId =>
        for {
          projectList <- projectRepo.deleteProject(projectId)
          response <- Ok()
        } yield response
      case GET -> Root / IntVar(projectId) as userId =>
        for {
          project <- projectRepo.getProject(projectId)
          response <- Ok(project.asJson)
        } yield response
      case authReq @ POST -> Root as userId =>
        for {
          newProject <-
            authReq.req
              .asJsonDecode[NewProject]
              .adaptError(_ => BadRequestError)
          project <-
            projectRepo
              .createProject(newProject, userId)
              .adaptError {
                case x: PSQLException
                    if x.getMessage.contains(
                      "unique constraint"
                    ) =>
                  DuplicateNameError(newProject.name)
              }
          response <- Created(project.asJson)
        } yield response
      case authReq @ PUT -> Root / IntVar(projectId) as userId =>
        for {
          newProject <-
            authReq.req
              .asJsonDecode[NewProject]
              .adaptError(_ => BadRequestError)
          project <-
            projectRepo
              .renameProject(projectId, newProject, userId)
              .adaptError {
                case x
                    if x.getMessage.contains(
                      "unique constraint"
                    ) =>
                  DuplicateNameError(newProject.name)
              }
          response <- Ok(project.asJson)
        } yield response
    }
  }

  val routes = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}

object ProjectRoutes {
  def make[F[_]: Defer: MonadError[*[_], Throwable]: Sync](
      authMiddleware: AuthMiddleware[F, UserId],
      projectRepo: ProjectRepository[F]
  ) = Sync[F].delay { new ProjectRoutes(authMiddleware, projectRepo) }
}
