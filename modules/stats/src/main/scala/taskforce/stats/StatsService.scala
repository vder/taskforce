package taskforce.stats


final class StatsService[F[_]] private (statsRepo: StatsRepository[F]) {
  def getStats(query: StatsQuery) = statsRepo.get(query)
}

object StatsService {
  def make[F[_]](statsRepo: StatsRepository[F]) =
    new StatsService[F](statsRepo)

}
