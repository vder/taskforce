package taskforce.infrastructure

import cats.effect.Sync
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
    Db(
      FilterRepository.make[F](xa),
      ProjectRepository.make[F](xa),
      StatsRepository.make[F](xa),
      TaskRepository.make[F](xa),
      UserRepository.make[F](xa)
    )
}
