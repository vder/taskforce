package taskforce

import cats.effect._
import doobie.hikari._
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._
import taskforce.authentication.TaskForceAuthMiddleware
import taskforce.config.{DatabaseConfig, HostConfig}
import taskforce.infrastructure.{Db, Server}

object Main extends IOApp {

  implicit def unsafeLogger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

  val resources: Resource[IO, (HikariTransactor[IO], HostConfig)] =
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
      _ <- flywayMigrate(dbConfig)
    } yield (xa, hostConfig)

  override def run(args: List[String]): IO[ExitCode] = {
    val serverResource = for {
      (xa, hostConfig) <- resources
      db             = Db.make[IO](xa)
      authMiddleware = TaskForceAuthMiddleware(db.userRepo, hostConfig.secret.value)
      server <- Server.resource(hostConfig.port, authMiddleware, db)
    } yield server

    serverResource.use(_ => IO.never)
  }

  private def flywayMigrate(db: DatabaseConfig): Resource[IO, MigrateResult] =
    Resource.eval(IO {
      Flyway
        .configure()
        .dataSource(db.url.value, db.user.value, db.pass.value)
        .cleanDisabled(true)
        .load()
        .migrate()
    })

}
