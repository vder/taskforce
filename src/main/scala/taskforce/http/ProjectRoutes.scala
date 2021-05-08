package taskforce.http

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Defer, MonadError}
import io.circe.syntax._
import org.http4s.AuthedRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import taskforce.model._
import taskforce.model.errors._
import taskforce.repos.ProjectRepository

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
          response    <- Ok(projectList.asJson)
        } yield response

      case DELETE -> Root / LongVar(projectId) as userId =>
        for {
          projectOption <- projectRepo.getProject(ProjectId(projectId))
          project <-
            MonadError[F, Throwable]
              .fromOption(
                projectOption,
                NotFoundError(ProjectId(projectId))
              )
              .ensure(NotAuthorError(userId))(_.author == userId)
          _        <- projectRepo.deleteProject(ProjectId(projectId))
          response <- Ok()
        } yield response

      case GET -> Root / LongVar(projectId) as userId =>
        val id = ProjectId(projectId)
        for {
          project <-
            projectRepo
              .getProject(id)
              .ensure(NotFoundError(id))(_.isDefined)
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

          response <- Created(project.asJson)
        } yield response
      case authReq @ PUT -> Root / IntVar(projectId) as userId =>
        val id = ProjectId(projectId)
        for {
          newProject <-
            authReq.req
              .asJsonDecode[NewProject]
              .adaptError(_ => BadRequestError)
          previousProject <-
            projectRepo
              .getProject(ProjectId(projectId))
              .ensure(NotFoundError(id))(_.isDefined)
              .ensure(NotAuthorError(userId))(_.filter(_.author == userId).isDefined)
          project <-
            projectRepo
              .renameProject(ProjectId(projectId), newProject)
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
