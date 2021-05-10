package taskforce.http

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Defer, MonadError}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.AuthedRoutes
import taskforce.model._
import io.chrisdavenport.log4cats.Logger
import taskforce.repos.StatsRepository
import org.http4s.EntityDecoder

final class StatsRoutes[
    F[_]: Sync: Applicative: MonadError[
      *[_],
      Throwable
    ]: JsonDecoder: Logger
](
    authMiddleware: AuthMiddleware[F, UserId],
    statsRepo: StatsRepository[F]
) {

  private[this] val prefixPath = "/api/v1/stats"

  implicit def decodeStats: EntityDecoder[F, StatsResponse] = jsonOf
  //implicit def encodeStats: EntityDecoder[F, StatsResponse] = jsonEncoderOf

  val httpRoutes: AuthedRoutes[UserId, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    AuthedRoutes.of {
      case authReq @ GET -> Root as userId =>
        for {
          query <-
            authReq.req
              .asJsonDecode[StatsQuery]
              .recoverWith(x => Logger[F].warn(x.getMessage()) *> StatsQuery(List(), None, None).pure[F])
          _ <- Logger[F].info(query.toString())
          stats <-
            statsRepo
              .getStats(query)
          response <- Ok(stats)
        } yield response

    }
  }

  val routes = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}

object StatsRoutes {
  def make[F[_]: Defer: MonadError[*[_], Throwable]: Sync: Logger](
      authMiddleware: AuthMiddleware[F, UserId],
      statsRepo: StatsRepository[F]
  ) = Sync[F].delay { new StatsRoutes(authMiddleware, statsRepo) }
}
