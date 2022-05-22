package taskforce.stats

import cats.effect.Sync

final class StatsService[F[_]](
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
