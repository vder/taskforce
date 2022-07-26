package taskforce.infrastructure

import cats.effect.Sync
import cats.implicits._
import doobie.util.transactor.Transactor
import taskforce.authentication.UserRepository
import taskforce.filter.FilterRepository
import taskforce.project.ProjectRepository
import taskforce.stats.StatsRepository
import taskforce.task.TaskRepository
import org.typelevel.log4cats.Logger

final case class Db[F[_]](
    filterRepo: FilterRepository[F],
    projectRepo: ProjectRepository[F],
    statsRepo: StatsRepository[F],
    taskRepo: TaskRepository[F],
    userRepo: UserRepository[F]
)

object Db {
  def make[F[_]: Sync: Logger](xa: Transactor[F]) =
    for {
      filterDb  <- FilterRepository.make[F](xa).pure[F]
      projectDb <- ProjectRepository.make[F](xa).pure[F]
      statsDb   <- StatsRepository.make[F](xa).pure[F]
      taskDb    <- TaskRepository.make[F](xa).pure[F]
      userdDb   <- UserRepository.make[F](xa).pure[F]
    } yield Db(filterDb, projectDb, statsDb, taskDb, userdDb)
}
