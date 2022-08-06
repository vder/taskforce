package taskforce.stats

import cats.effect.kernel.Async
import org.http4s.server.Router
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import taskforce.authentication.Authenticator
import taskforce.common.BaseApi

final class StatsRoutes[F[_]: Async] private (
    authenticator: Authenticator[F],
    statsService: StatsService[F]
) extends instances.Circe
    with instances.TapirCodecs {

  private[this] val prefixPath = "/api/v1/stats"

  object endpoints {
    val stats =
      authenticator
        .secureEndpoints(BaseApi.endpoint)
        .get
        .in(jsonBody[StatsQuery])
        .out(jsonBody[StatsResponse])
        .serverLogicSuccess(_ =>
          query =>
            statsService
              .getStats(query)
        )
  }

  def routes =
    Router(
      prefixPath -> Http4sServerInterpreter[F]().toRoutes(endpoints.stats)
    )
}

object StatsRoutes {
  def make[F[_]: Async](
      authenticator: Authenticator[F],
      statsService: StatsService[F]
  ) = new StatsRoutes(authenticator, statsService)
}
