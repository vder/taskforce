package taskforce.stats

import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import taskforce.common.Sqlizer.ops._
import cats.effect.kernel.MonadCancelThrow
trait StatsRepository[F[_]] {
  def get(query: StatsQuery): F[StatsResponse]
}

object StatsRepository {
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]) =
    new StatsRepository[F] with instances.Doobie {

      override def get(query: StatsQuery): F[StatsResponse] =
        sql
          .getStats(query)
          .query[StatsResponse]
          .unique
          .transact(xa)

      private object sql {
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
}
