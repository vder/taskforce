package taskforce

import cats.effect._
import cats._
import cats.implicits._
import scala.concurrent.ExecutionContext.global
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import doobie.hikari._
import org.http4s.implicits._
import org.http4s.server.blaze._
import taskforce.http.BasicRoutes
import java.util.UUID
import doobie.util.ExecutionContexts
import taskforce.repository.LiveUserRepository
import org.http4s.server.AuthMiddleware
import taskforce.http.TaskForceAuthMiddleware

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

  val user = UUID.fromString("5260ca29-a70b-494e-a3d6-55374a3b0a04")

  override def run(args: List[String]): IO[ExitCode] =
    transactor
      .use { xa =>
        for {
          db <- LiveUserRepository.make[IO](xa)
          //auth <- TestAuth.make[IO](user)
          authRepo <- LiveAuth.make[IO](db)
          authMiddleware = TaskForceAuthMiddleware.middleware[IO](authRepo)
          routes <- BasicRoutes.make[IO](authMiddleware)
          httpApp = (routes.routes).orNotFound
          _ <-
            BlazeServerBuilder[IO](global)
              .bindHttp(
                port.getOrElse(args.head.toInt),
                "0.0.0.0"
              )
              .withHttpApp(httpApp)
              .serve
              .compile
              .drain
        } yield ExitCode.Success
      }
}
