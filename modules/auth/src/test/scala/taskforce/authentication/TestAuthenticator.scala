package taskforce.auth

import taskforce.authentication.Authenticator
import cats.effect.IO
import sttp.tapir.Endpoint
import sttp.tapir.auth
import sttp.tapir.server.PartialServerEndpoint
import taskforce.authentication.UserId
import taskforce.common.ResponseError
import cats.implicits._

object TestAuthenticator {

  def apply(userId: UserId): Authenticator[IO] =
    new Authenticator[IO] {
      override def secureEndpoints[SECURITY_INPUT, INPUT, OUTPUT](
          endpoints: Endpoint[Unit, INPUT, ResponseError, OUTPUT, Any]
      ): PartialServerEndpoint[String, UserId, INPUT, ResponseError, OUTPUT, Any, IO] =
        endpoints
          .securityIn(auth.bearer[String]())
          .serverSecurityLogic { _ => userId.asRight[ResponseError].pure[IO] }

    }
}
