package taskforce.stats

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Defer, MonadError}
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger
import taskforce.authentication.UserId
import taskforce.common.{ErrorHandler, errors => commonErrors}
final class StatsRoutes[
    F[_]: Sync: Applicative: MonadError[
      *[_],
      Throwable
    ]: JsonDecoder: Logger
](
    authMiddleware: AuthMiddleware[F, UserId],
    statsService: StatsService[F]
) extends instances.Circe {

  private[this] val prefixPath = "/api/v1/stats"

  val httpRoutes: AuthedRoutes[UserId, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    AuthedRoutes.of {
      case authReq @ GET -> Root as userId =>
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
  def make[F[_]: Defer: MonadError[*[_], Throwable]: Sync: Logger: JsonDecoder](
      authMiddleware: AuthMiddleware[F, UserId],
      statsService: StatsService[F]
  ) = Sync[F].delay { new StatsRoutes(authMiddleware, statsService) }
}
