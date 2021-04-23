package taskforce.http

import cats.implicits._

import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import cats.effect.Sync
import cats.Defer
import cats.MonadError
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s.AuthedRoutes
import taskforce.model.domain._
import cats.Applicative
import cats.Monad
import org.http4s.server.AuthMiddleware

final class BasicRoutes[F[_]: Defer: Applicative: Monad](
    authMiddleware: AuthMiddleware[F, UserId]
) {

  private[this] val prefixPath = "/api/v1/"

  val httpRoutes: AuthedRoutes[UserId, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    AuthedRoutes.of {
      case GET -> Root / "test" as userId =>
        for {
          response <- Ok(s"its alive ${userId}")
        } yield response

    }
  }

  val routes = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}

object BasicRoutes {
  def make[F[_]: Defer: MonadError[*[_], Throwable]: Sync](
      authMiddleware: AuthMiddleware[F, UserId]
  ) =
    Sync[F].delay { new BasicRoutes(authMiddleware) }
}
