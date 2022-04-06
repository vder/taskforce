package taskforce.infrastructure

import cats.effect.kernel.Async
import cats.effect.{ExitCode, Sync}
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.middleware.{AutoSlash, Logger => LoggerMiddleware}
import org.typelevel.log4cats.Logger
import taskforce.authentication.UserId
import taskforce.common._
import taskforce.filter.{FilterRoutes, FilterService}
import taskforce.project.{ProjectRoutes, ProjectService}
import taskforce.stats.{StatsRoutes, StatsService}
import taskforce.task.{TaskRoutes, TaskService}
import com.comcast.ip4s._
import org.http4s.ember.server._

final class Server[F[_]: Logger: Async] private (
    port: Int,
    authMiddleware: AuthMiddleware[F, UserId],
    db: Db[F]
) {

  def run /*(implicit T: Temporal[F])*/ =
    for {
      basicRoutes    <- BasicRoutes.make(authMiddleware)
      projectService <- ProjectService.make(db.projectRepo)
      taskService    <- TaskService.make(db.taskRepo)
      statsService   <- StatsService.make(db.statsRepo)
      filterService  <- FilterService.make(db.filterRepo)
      projectRoutes  <- ProjectRoutes.make(authMiddleware, projectService)
      filterRoutes   <- FilterRoutes.make(authMiddleware, filterService)
      statsRoutes    <- StatsRoutes.make(authMiddleware, statsService)
      taskRoutes     <- TaskRoutes.make(authMiddleware, taskService)
      errHandler = LiveHttpErrorHandler[F]
      routes =
        basicRoutes.routes <+> projectRoutes.routes(errHandler) <+>
          taskRoutes.routes(errHandler) <+> filterRoutes.routes(errHandler) <+>
          statsRoutes.routes(errHandler)
      middlewares =
        LoggerMiddleware
          .httpRoutes[F](logHeaders = true, logBody = true) _ andThen AutoSlash.httpRoutes[F]
      portChecked <- Port.fromInt(port).liftTo[F](errors.InvalidPort(port))    
      exitCode <-
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(portChecked)
          .withHttpApp(middlewares(routes).orNotFound)
          .build
          .use( _ => Async[F].never.as(ExitCode.Success))
          
    } yield exitCode

}

object Server {
  def make[F[_]: Async: Logger](
      port: Int,
      authMiddleware: AuthMiddleware[F, UserId],
      db: Db[F]
  ): F[Server[F]] =
    Sync[F].delay(new Server(port, authMiddleware, db))
}
