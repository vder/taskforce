package taskforce.infrastructure

import cats.effect.Sync
import cats.implicits._
import doobie.util.transactor.Transactor
import taskforce.authentication.{UserRepository, LiveUserRepository}
import taskforce.filter.{FilterRepository, LiveFilterRepository}
import taskforce.project.{ProjectRepository, LiveProjectRepository}
import taskforce.stats.{StatsRepository, LiveStatsRepository}
import taskforce.task.{TaskRepository, LiveTaskRepository}
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
      filterDb <- LiveFilterRepository.make[F](xa)
      projectDb <- LiveProjectRepository.make[F](xa)
      statsDb <- LiveStatsRepository.make[F](xa)
      taskDb <- LiveTaskRepository.make[F](xa)
      userdDb <- LiveUserRepository.make[F](xa)
    } yield Db(filterDb, projectDb, statsDb, taskDb, userdDb)
}
