package taskforce.infrastructure

import cats.effect.kernel.Async
import cats.effect.ExitCode
import cats.implicits._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.middleware.{AutoSlash, Logger => LoggerMiddleware}
import org.typelevel.log4cats.Logger
import scala.concurrent.ExecutionContext.global
import taskforce.authentication.UserId
import taskforce.common._
import taskforce.filter.{FilterRoutes, FilterService}
import taskforce.project.{ProjectRoutes, ProjectService}
import taskforce.stats.{StatsRoutes, StatsService}
import taskforce.task.{TaskRoutes, TaskService}
import taskforce.authentication.Authenticator

final class Server[F[_]: Logger: Async] private (
    port: Int,
    authMiddleware: AuthMiddleware[F, UserId],
    authenticator: Authenticator[F],
    db: Db[F]
) {

  def run =
    for {
      basicRoutes    <- BasicRoutes.make(authenticator).pure[F]
      projectService <- ProjectService.make(db.projectRepo).pure[F]
      taskService    <- TaskService.make(db.taskRepo).pure[F]
      statsService   <- StatsService.make(db.statsRepo).pure[F]
      filterService  <- FilterService.make(db.filterRepo).pure[F]
      projectRoutes  <- ProjectRoutes.make(authenticator, projectService).pure[F]
      filterRoutes   <- FilterRoutes.make(authMiddleware, filterService).pure[F]
      statsRoutes    <- StatsRoutes.make(authMiddleware, statsService).pure[F]
      taskRoutes     <- TaskRoutes.make(authenticator, taskService).pure[F]
      errHandler     <- ErrorHandler[F].pure[F]
      routes =
        basicRoutes.routes <+> projectRoutes.routes <+>
          taskRoutes.routes <+> filterRoutes.routes(errHandler) <+>
          statsRoutes.routes(errHandler)
      middlewares =
        LoggerMiddleware
          .httpRoutes[F](logHeaders = true, logBody = true) _ andThen AutoSlash.httpRoutes[F]
      _ <-
        BlazeServerBuilder[F]
          .withExecutionContext(global)
          .bindHttp(port, "0.0.0.0")
          .withHttpApp(middlewares(routes).orNotFound)
          .serve
          .compile
          .drain
    } yield ExitCode.Success

}

object Server {
  def make[F[_]: Async: Logger](
      port: Int,
      authMiddleware: AuthMiddleware[F, UserId],
      authenticator: Authenticator[F],
      db: Db[F]
  ): Server[F] =
    new Server(port, authMiddleware, authenticator, db)
}
