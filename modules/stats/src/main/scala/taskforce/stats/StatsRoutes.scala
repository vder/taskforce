package taskforce.stats

import cats.implicits._
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger
import taskforce.authentication.UserId
import taskforce.common.{ErrorHandler, errors => commonErrors}
import cats.MonadThrow

final class StatsRoutes[F[_]: MonadThrow: JsonDecoder: Logger] private (
    authMiddleware: AuthMiddleware[F, UserId],
    statsService: StatsService[F]
) extends instances.Circe {

  private[this] val prefixPath = "/api/v1/stats"

  val httpRoutes: AuthedRoutes[UserId, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    AuthedRoutes.of { case authReq @ GET -> Root as _ =>
      for {
        query <-
          authReq.req
            .asJsonDecode[StatsQuery]
            .adaptError(_ => commonErrors.BadRequest)
        _ <- Logger[F].info(query.toString())
        stats <-
          statsService
            .getStats(query)
        response <- Ok(stats)
      } yield response

    }
  }

  def routes(errHandler: ErrorHandler[F, Throwable]) =
    Router(
      prefixPath -> errHandler.basicHandle(authMiddleware(httpRoutes))
    )
}

object StatsRoutes {
  def make[F[_]: MonadThrow: Logger: JsonDecoder](
      authMiddleware: AuthMiddleware[F, UserId],
      statsService: StatsService[F]
  ) = new StatsRoutes(authMiddleware, statsService)
}
