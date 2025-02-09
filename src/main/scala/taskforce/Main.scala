package taskforce

import cats.effect._
import cats.implicits._
import doobie.hikari._
import doobie.util.ExecutionContexts
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import taskforce.authentication.TaskForceAuthMiddleware
import taskforce.config.DatabaseConfig
import taskforce.config.HostConfig
import taskforce.infrastructure.Db
import taskforce.infrastructure.Server
import cats.effect.Resource
import org.flywaydb.core.Flyway
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.flywaydb.core.api.output.MigrateResult


object Main extends IOApp {

  implicit def unsafeLogger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

  val resources: Resource[IO, (HikariTransactor[IO], HostConfig, DatabaseConfig)] =
    for {
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
    } yield (xa, hostConfig, dbConfig)

  override def run(args: List[String]): IO[ExitCode] =
    resources
      .use { case (xa, hostConfig, dbConfig) =>
        for {
          _ <- flywayMigrate(dbConfig)
          db             <- Db.make[IO](xa)
          authMiddleware <- TaskForceAuthMiddleware(db.userRepo, hostConfig.secret.value).pure[IO]
          server <- Server
            .make[IO](
              hostConfig.port.value,
              authMiddleware,
              db
            )
            .pure[IO]
          exitCode <- server.run
        } yield exitCode
      }
      
  private def flywayMigrate(db: DatabaseConfig): IO[MigrateResult] =
    IO {
      Flyway
        .configure()
        .dataSource(db.url.value, db.user.value, db.pass.value)
        .cleanDisabled(true)
        .load()
        .migrate()
    }
}
