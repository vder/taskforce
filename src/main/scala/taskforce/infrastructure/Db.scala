package taskforce.infrastructure

import doobie.util.transactor.Transactor
import taskforce.authentication.UserRepository
import taskforce.filter.FilterRepository
import taskforce.project.ProjectRepository
import taskforce.stats.StatsRepository
import taskforce.task.TaskRepository
import org.typelevel.log4cats.Logger
import cats.effect.kernel.MonadCancelThrow
import cats.effect.kernel.Clock

final case class Db[F[_]](
    filterRepo: FilterRepository[F],
    projectRepo: ProjectRepository[F],
    statsRepo: StatsRepository[F],
    taskRepo: TaskRepository[F],
    userRepo: UserRepository[F]
)

object Db {
  def make[F[_]: MonadCancelThrow: Logger: Clock](xa: Transactor[F]): Db[F] =
    Db(
      FilterRepository.make[F](xa),
      ProjectRepository.make[F](xa),
      StatsRepository.make[F](xa),
      TaskRepository.make[F](xa),
      UserRepository.make[F](xa)
    )
}
