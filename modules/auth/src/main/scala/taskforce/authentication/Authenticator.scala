package taskforce.authentication

import taskforce.authentication.instances.Circe
import sttp.tapir.server._
import sttp.tapir._

import taskforce.common.ResponseError
//import cats.Monad

trait Authenticator[F[_]] {

  def secureEndpoints[SECURITY_INPUT, INPUT, OUTPUT](
      endpoints: Endpoint[Unit, INPUT, ResponseError, OUTPUT, Any]
  ): PartialServerEndpoint[String, UserId, INPUT, ResponseError, OUTPUT, Any, F]
}

object Authenticator {

  def make[F[_]](
      authService: AuthService[F]
  ) = new Authenticator[F] with Circe {

    def secureEndpoints[SECURITY_INPUT, INPUT, OUTPUT](
        endpoints: Endpoint[Unit, INPUT, ResponseError, OUTPUT, Any]
    ): PartialServerEndpoint[String, UserId, INPUT, ResponseError, OUTPUT, Any, F] =
      endpoints
        .securityIn(auth.bearer[String]())
        .serverSecurityLogic(s => authService.authenticate(s))

  }
}
