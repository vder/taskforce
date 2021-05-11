package taskforce

import cats.effect._
import cats.implicits._
import doobie.hikari._
import doobie.util.ExecutionContexts
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.implicits._
import org.http4s.server.blaze._
import scala.concurrent.ExecutionContext.global
import taskforce.http.{
  TaskForceAuthMiddleware,
  ProjectRoutes,
  TaskRoutes,
  LiveHttpErrorHandler,
  BasicRoutes,
  FilterRoutes
}
import taskforce.repos._
import pureconfig.ConfigSource
import taskforce.config.DatabaseConfig
import pureconfig.module.catseffect.syntax._
import taskforce.http.StatsRoutes
import taskforce.config.HostConfig

object Main extends IOApp {

  (2.pure[IO], 3.pure[IO]).mapN(_ + _)

  val resources: Resource[IO, (HikariTransactor[IO], HostConfig)] =
    for {
      be <- Blocker[IO]
      dbConfig <- Resource.eval(
        ConfigSource.default.at("database").loadF[IO, DatabaseConfig](be)
      )
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        dbConfig.driver.value,
        dbConfig.url.value,
        dbConfig.user.value,
        dbConfig.pass.value,
        ce, // await connection here
        be  // execute JDBC operations here
      )
      hostConfig <- Resource.eval(
        ConfigSource.default.at("host").loadF[IO, HostConfig](be)
      )
    } yield (xa, hostConfig)

  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  val port   = sys.env.get("PORT").flatMap(_.toIntOption)
  val secret = sys.env.get("SECRET")

  override def run(args: List[String]): IO[ExitCode] =
    resources
      .use {
        case (xa, hostConfig) =>
          for {
            db       <- Db.make[IO](xa)
            authRepo <- LiveAuth.make[IO](db.userRepo, secret.getOrElse(hostConfig.secret.value))
            authMiddleware = TaskForceAuthMiddleware.middleware[IO](authRepo)
            basicRoutes   <- BasicRoutes.make[IO](authMiddleware)
            projectRoutes <- ProjectRoutes.make(authMiddleware, db.projectRepo)
            filterRoutes  <- FilterRoutes.make(authMiddleware, db.filterRepo)
            statsRoutes   <- StatsRoutes.make(authMiddleware, db.statsRepo)
            taskRoutes    <- TaskRoutes.make(authMiddleware, db.taskRepo)
            routes = LiveHttpErrorHandler[IO].handle(
              basicRoutes.routes <+> projectRoutes.routes <+> taskRoutes.routes <+> filterRoutes.routes <+> statsRoutes.routes
            )
            httpApp = (routes).orNotFound
            _ <-
              BlazeServerBuilder[IO](global)
                .bindHttp(
                  port.getOrElse(hostConfig.port.value),
                  "0.0.0.0"
                )
                .withHttpApp(httpApp)
                .serve
                .compile
                .drain
          } yield ExitCode.Success
      }
}
