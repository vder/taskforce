package taskforce.stats

import cats.Monad
import cats.effect.Sync
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import taskforce.common.Sqlizer.ops._
import cats.effect.kernel.MonadCancel
trait StatsRepository[F[_]] {
  def get(query: StatsQuery): F[StatsResponse]
}

final class LiveStatsRepository[F[_]: Monad: MonadCancel[*[_], Throwable]](
    xa: Transactor[F]
) extends StatsRepository[F]
    with instances.Doobie {

  override def get(query: StatsQuery): F[StatsResponse] =
    sql
      .getStats(query)
      .query[StatsResponse]
      .unique
      .transact(xa)

  object sql {
    def getStats(query: StatsQuery) = {
      fr"""select count(*) cnt,
           |      avg(duration) avg_duration,
           |      avg(volume) avg_volume,
           |      sum(duration* volume)/sum(volume) avg_volume_duration
           | from tasks t
           |where t.deleted is null
           |  and t.started between coalesce(${query.from},t.started) and coalesce(${query.to},t.started)
           |""".stripMargin ++ query.toFragment
    }
  }
}

object LiveStatsRepository {
  def make[F[_]: Sync: Monad: MonadCancel[*[_], Throwable]](xa: Transactor[F]) =
    Sync[F].delay { new LiveStatsRepository[F](xa) }
}
