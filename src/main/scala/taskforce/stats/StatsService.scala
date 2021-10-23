package taskforce.stats

import cats.effect.Sync
import cats.{Applicative, MonadError}

final class StatsService[F[_]: Sync: Applicative: MonadError[*[_], Throwable]](
    statsRepo: StatsRepository[F]
) {
  def getStats(query: StatsQuery) = statsRepo.get(query)
}

object StatsService {
  def make[F[_]: Sync](statsRepo: StatsRepository[F]) =
    Sync[F].delay(
      new StatsService[F](statsRepo)
    )
}
