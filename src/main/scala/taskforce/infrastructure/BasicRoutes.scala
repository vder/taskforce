package taskforce.infrastructure

import cats.effect.Sync
import cats.implicits._
import org.http4s.AuthedRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import taskforce.authentication.UserId
import org.http4s.HttpRoutes

final class BasicRoutes[F[_]: Sync](
    authMiddleware: AuthMiddleware[F, UserId]
) {

  private[this] val prefixPath = "/api/v1/"

  val httpRoutes: AuthedRoutes[UserId, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    AuthedRoutes.of { case GET -> Root / "testAuth" as userId =>
      for {
        response <- Ok(s"its alive ${userId}")
      } yield response

    }
  }

  val basicRoutes = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
   HttpRoutes.of[F] {
      case GET -> Root / "test" =>
        Ok("its alive")
    }
  }

  val routes = Router(
    prefixPath -> authMiddleware(httpRoutes),
    prefixPath -> basicRoutes
  )
}

object BasicRoutes {
  def make[F[_]: Sync](
      authMiddleware: AuthMiddleware[F, UserId]
  ) =
    Sync[F].delay { new BasicRoutes(authMiddleware) }
}
