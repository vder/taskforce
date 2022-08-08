package taskforce.common

import org.http4s.HttpRoutes
import sttp.tapir.server.http4s.Http4sServerInterpreter
import cats.effect.kernel.Async
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import cats.implicits._
import sttp.tapir.server.ServerEndpoint
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.swagger.SwaggerUIOptions

trait DefaultEndpointInterpreter {

  def toRoutes[F[_]: Async](path: String)(se: ServerEndpoint[Fs2Streams[F], F]*): HttpRoutes[F] = {

    val endpoints       = se.toList
    val httpInterpreter = Http4sServerInterpreter[F]()

    val options = SwaggerUIOptions.default.copy(pathPrefix = List("docs", path))

    val swaggerEndpoints = httpInterpreter.toRoutes(
      SwaggerInterpreter(swaggerUIOptions = options).fromEndpoints[F](endpoints.map(_.endpoint), "Taskforce", "1.0")
    )

    swaggerEndpoints <+> httpInterpreter.toRoutes(endpoints)
  }

}
