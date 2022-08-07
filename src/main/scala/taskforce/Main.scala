package taskforce

import cats.effect._
import cats.implicits._
import doobie.hikari._
import doobie.util.ExecutionContexts
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import taskforce.config.DatabaseConfig
import taskforce.config.HostConfig
import taskforce.infrastructure.Db
import taskforce.infrastructure.Server
import cats.effect.Resource
import org.typelevel.log4cats.slf4j.Slf4jLogger
import taskforce.authentication.Authenticator
import taskforce.authentication.AuthService


object Main extends IOApp {

  implicit def unsafeLogger[F[_]: Sync] = Slf4jLogger.getLogger[F]

  val resources: Resource[IO, (HikariTransactor[IO], HostConfig)] =
    for {
      //be <- Resource.unit[IO]
      dbConfig <- Resource.eval(
        ConfigSource.default.at("database").loadF[IO, DatabaseConfig]()
      )
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        dbConfig.driver.value,
        dbConfig.url.value,
        dbConfig.user.value,
        dbConfig.pass.value,
        ce // await connection here
      )
      hostConfig <- Resource.eval(
        ConfigSource.default.at("host").loadF[IO, HostConfig]()
      )
    } yield (xa, hostConfig)

  override def run(args: List[String]): IO[ExitCode] =
    resources
      .use { case (xa, hostConfig) =>
        for {
          db             <- Db.make[IO](xa)
          authService <- AuthService(db.userRepo, hostConfig.secret.value).pure[IO]
          authEndpoint <- Authenticator.make[IO](authService).pure[IO]
          server <- Server.make[IO](
            hostConfig.port.value,
            authEndpoint,
            db
          ).pure[IO]
          exitCode <- server.run
        } yield exitCode
      }
}
