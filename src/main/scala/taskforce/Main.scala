package taskforce

import cats._
import cats.effect._
import cats.implicits._
import doobie.hikari._
import doobie.util.ExecutionContexts
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import java.util.UUID
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.blaze._
import scala.concurrent.ExecutionContext.global
import taskforce.http.{
  TaskForceAuthMiddleware,
  ProjectRoutes,
  TaskRoutes,
  LiveHttpErrorHandler,
  BasicRoutes
}
import taskforce.repository.LiveUserRepository
import taskforce.repository._

object Main extends IOApp {

  (2.pure[IO], 3.pure[IO]).mapN(_ + _)

  val transactor: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
      be <- Blocker[IO] // our blocking EC
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver", // driver classname
        "jdbc:postgresql://localhost:54340/exchange", // connect URL
        "vder", // username
        "gordon", // password
        ce, // await connection here
        be // execute JDBC operations here
      )
    } yield xa

  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  val port = sys.env.get("PORT").flatMap(_.toIntOption)

  override def run(args: List[String]): IO[ExitCode] =
    transactor
      .use { xa =>
        for {
          db <- LiveUserRepository.make[IO](xa)
          projectDb <- LiveProjectRepository.make[IO](xa)
          taskDb <- LiveTaskRepository.make[IO](xa)
          authRepo <- LiveAuth.make[IO](db)
          authMiddleware = TaskForceAuthMiddleware.middleware[IO](authRepo)
          basicRoutes <- BasicRoutes.make[IO](authMiddleware)
          projectRoutes <- ProjectRoutes.make(authMiddleware, projectDb)
          taskRoutes <- TaskRoutes.make(authMiddleware, projectDb, taskDb)
          routes = LiveHttpErrorHandler[IO].handle(
            basicRoutes.routes <+> projectRoutes.routes <+> taskRoutes.routes
          )
          httpApp = (routes).orNotFound
          _ <-
            BlazeServerBuilder[IO](global)
              .bindHttp(
                port.getOrElse(
                  args.headOption.flatMap(_.toIntOption).getOrElse(9090)
                ),
                "0.0.0.0"
              )
              .withHttpApp(httpApp)
              .serve
              .compile
              .drain
        } yield ExitCode.Success
      }
}
