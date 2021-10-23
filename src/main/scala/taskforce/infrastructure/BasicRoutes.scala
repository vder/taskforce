package taskforce.infrastructure

import cats.effect.Sync
import cats.implicits._
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import taskforce.authentication.UserId

final class BasicRoutes[F[_]: Sync](
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
  def make[F[_]: Sync](
      authMiddleware: AuthMiddleware[F, UserId]
  ) =
    Sync[F].delay { new BasicRoutes(authMiddleware) }
}
