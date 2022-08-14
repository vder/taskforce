package taskforce.stats

import cats.effect.kernel.Async
import org.http4s.server.Router
import sttp.tapir.json.circe._
import taskforce.authentication.Authenticator
import taskforce.common.BaseEndpoint
import org.http4s.HttpRoutes
import taskforce.common.DefaultEndpointInterpreter
import sttp.tapir._
import taskforce.authentication.UserId
import cats.data.NonEmptyList

final class StatsRoutes[F[_]: Async] private (
    authenticator: Authenticator[F],
    statsService: StatsService[F]
) extends instances.Circe
    with instances.TapirCodecs
    with BaseEndpoint
    with DefaultEndpointInterpreter {

  private object endpoints {
    private val base = endpoint.in("stats")

    val stats =
      authenticator
        .secureEndpoint(base)
        .get
        .in(query[NonEmptyList[UserId]]("users").description("List of users id which we are calculate stats for"))
        .in(query[Option[DateFrom]]("from").description("Start of the period in format YYYY-DD-MM"))
        .in(query[Option[DateTo]]("to").description("End of the period in format YYYY-DD-MM"))
        .out(jsonBody[StatsResponse].description("""Stats forspecified params containing:  
                                                                      |<ul>
                                                                      |<li>number of tasks</li>
                                                                      |<li>average time of the single task</li>
                                                                      |<li>average volume(weight) of the task</li>
                                                                      |<li>average time of the single task weight by volume</li>
                                                                      |</ul>""".stripMargin))
        .serverLogicSuccess(_ => { case (users, from, to) =>
          statsService
            .getStats(StatsQuery(users.toList, from, to))
        })

    def routes: HttpRoutes[F] = toRoutes("stats")(stats)
  }

  def routes: HttpRoutes[F] =
    Router(
      "/" -> endpoints.routes
    )
}

object StatsRoutes {
  def make[F[_]: Async](
      authenticator: Authenticator[F],
      statsService: StatsService[F]
  ): StatsRoutes[F] = new StatsRoutes(authenticator, statsService)
}
