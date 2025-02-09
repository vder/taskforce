package taskforce.infrastructure

import cats.effect.kernel.Async
import cats.effect.Resource
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.middleware.{AutoSlash, Logger => LoggerMiddleware}
import org.typelevel.log4cats.Logger
import com.comcast.ip4s._
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import taskforce.authentication.UserId
import taskforce.common._
import taskforce.filter.{FilterRoutes, FilterService}
import taskforce.project.{ProjectRoutes, ProjectService}
import taskforce.stats.{StatsRoutes, StatsService}
import taskforce.task.{TaskRoutes, TaskService}

final class Server[F[_]: Logger: Async: Network] private (
    port: Port,
    authMiddleware: AuthMiddleware[F, UserId],
    db: Db[F]
) {

  private def resource: Resource[F, org.http4s.server.Server] = {
    val basicRoutes    = BasicRoutes.make(authMiddleware)
    val projectService = ProjectService.make(db.projectRepo)
    val taskService    = TaskService.make(db.taskRepo)
    val statsService   = StatsService.make(db.statsRepo)
    val filterService  = FilterService.make(db.filterRepo)
    val projectRoutes  = ProjectRoutes.make(authMiddleware, projectService)
    val filterRoutes   = FilterRoutes.make(authMiddleware, filterService)
    val statsRoutes    = StatsRoutes.make(authMiddleware, statsService)
    val taskRoutes     = TaskRoutes.make(authMiddleware, taskService)
    val errHandler     = LiveHttpErrorHandler[F]
    val routes =
      basicRoutes.routes <+> projectRoutes.routes(errHandler) <+>
        taskRoutes.routes(errHandler) <+> filterRoutes.routes(errHandler) <+>
        statsRoutes.routes(errHandler)
    val middlewares =
      LoggerMiddleware
        .httpRoutes[F](logHeaders = true, logBody = true) _ andThen AutoSlash.httpRoutes[F]

    EmberServerBuilder
      .default[F]
      .withHost(ipv4"0.0.0.0")
      .withPort(port)
      .withHttpApp(middlewares(routes).orNotFound)
      .build
  }

}

object Server {

  def resource[F[_]: Async: Logger: Network](
      port: Port,
      authMiddleware: AuthMiddleware[F, UserId],
      db: Db[F]
  ) =
    new Server(port, authMiddleware, db).resource

}
