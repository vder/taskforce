package taskforce.http

import taskforce.Auth
import dev.profunktor.auth.JwtAuthMiddleware
import cats.MonadError

object TaskForceAuthMiddleware {

  def middleware[F[_]: MonadError[*[_], Throwable]](auth: Auth[F]) =
    JwtAuthMiddleware(auth.jwtAuth, auth.authenticate)
}
