package taskforce.infrastructure

import cats.effect.kernel.Async
import cats.effect.Resource
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Logger => LoggerMiddleware}
import org.typelevel.log4cats.Logger
import com.comcast.ip4s._
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import taskforce.filter.{FilterRoutes, FilterService}
import taskforce.project.{ProjectRoutes, ProjectService}
import taskforce.stats.{StatsRoutes, StatsService}
import taskforce.task.{TaskRoutes, TaskService}
import taskforce.authentication.Authenticator

final class Server[F[_]: Async: Logger: Network] private (
    port: Port,
    authenticator: Authenticator[F],
    db: Db[F]
) {

  private def resource: Resource[F, org.http4s.server.Server] = {
    val basicRoutes    = BasicRoutes.make(authenticator)
    val projectService = ProjectService.make(db.projectRepo)
    val taskService    = TaskService.make(db.taskRepo)
    val statsService   = StatsService.make(db.statsRepo)
    val filterService  = FilterService.make(db.filterRepo)
    val projectRoutes  = ProjectRoutes.make(authenticator, projectService)
    val filterRoutes   = FilterRoutes.make(authenticator, filterService)
    val statsRoutes    = StatsRoutes.make(authenticator, statsService)
    val taskRoutes     = TaskRoutes.make(authenticator, taskService)
    val routes =
      basicRoutes.routes <+> projectRoutes.routes <+>
        taskRoutes.routes <+> filterRoutes.routes <+>
        statsRoutes.routes
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
      authenticator: Authenticator[F],
      db: Db[F]
  ) =
    new Server(port, authenticator, db).resource

}
