package taskforce.http

import cats.MonadError
import dev.profunktor.auth.JwtAuthMiddleware
import taskforce.Auth

object TaskForceAuthMiddleware {

  def middleware[F[_]: MonadError[*[_], Throwable]](auth: Auth[F]) =
    JwtAuthMiddleware(auth.jwtAuth, auth.authenticate)
}
