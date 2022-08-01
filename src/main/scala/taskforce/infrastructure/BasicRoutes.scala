package taskforce.infrastructure

import cats.implicits._
import org.http4s.server.{Router}

import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import cats.effect.kernel.Async
import taskforce.common.ResponseError
import taskforce.authentication.Authenticator

final class BasicRoutes[F[_]: Async] private (authenticator: Authenticator[F]) {

  private[this] val prefixPath = "/api/v1/"

  private val testEndpoint: PublicEndpoint[Unit, Unit, String, Any] = endpoint.in("test").out(stringBody)

  private val authTestEndpoint = authenticator.secureEndpoint.get
    .in("testAuth")
    .out(stringBody)
    .serverLogicPure(userId => _ => s"its alive(tapir) $userId".asRight[ResponseError])

  private val tapirRoutes = Http4sServerInterpreter[F]().toRoutes(
    testEndpoint.serverLogic(_ => "its alive(tapir)".asRight[Unit].pure[F])
  ) <+> Http4sServerInterpreter[F]().toRoutes(authTestEndpoint)

  val routes = Router(prefixPath -> tapirRoutes)
}

object BasicRoutes {
  def make[F[_]: Async](authenticator: Authenticator[F]) =
    new BasicRoutes(authenticator)
}
