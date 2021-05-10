package taskforce.repos

import cats.Monad
import cats.effect.{Bracket, Sync}
import doobie.util.transactor.Transactor
import taskforce.model._
import cats.data.NonEmptyList
import doobie.util.fragment.Fragment
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._

trait StatsRepository[F[_]] {
  def getStats(query: StatsQuery): F[StatsResponse]
}

final class LiveStatsRepository[F[_]: Monad: Bracket[*[_], Throwable]](
    xa: Transactor[F]
) extends StatsRepository[F] {

  override def getStats(query: StatsQuery): F[StatsResponse] = {
    val inFragment =
      NonEmptyList
        .fromList(query.users)
        .map(x => fr" and " ++ Fragments.in(fr"t.author", x))
        .getOrElse(Fragment.empty)

    val sql = fr"""select count(*) cnt,
          |      avg(duration) avg_duration,
          |      avg(volume) avg_volume,
          |      avg(duration* volume) avg_volume_duration
          | from tasks t
          |where t.deleted is null
          |  and t.started between coalesce(${query.from},t.started) and coalesce(${query.to},t.started)
      """.stripMargin ++ inFragment

    sql
      .query[StatsResponse]
      .unique
      .transact(xa)
  }

}

object LiveStatsRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveStatsRepository[F](xa) }
}
