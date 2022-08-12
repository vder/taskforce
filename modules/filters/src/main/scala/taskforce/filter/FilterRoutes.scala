package taskforce.filter

import cats.implicits._
import sttp.tapir._
import sttp.tapir.json.circe._
import org.http4s.server.Router
import taskforce.filter.model._
import taskforce.authentication.Authenticator
import sttp.capabilities.fs2.Fs2Streams
import java.nio.charset.StandardCharsets
import org.http4s.HttpRoutes
import cats.effect.kernel.Async
import taskforce.common.ResponseError
import taskforce.common.StreamingResponse
import sttp.model.StatusCode
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats.effect
import sttp.tapir.server.http4s.Http4sServerOptions
import taskforce.common.DefaultEndpointInterpreter
import taskforce.common.BaseEndpoint

final class FilterRoutes[F[_]: Async: Logger] private (
    authenticator: Authenticator[F],
    filterService: FilterService[F]
) extends instances.Circe
    with instances.TapirCodecs
    with DefaultEndpointInterpreter
    with StreamingResponse
    with BaseEndpoint {

  implicit def unsafeLogger[IO] = Slf4jLogger.getLogger[effect.IO]

  private object endpoints {

    val base = endpoint.in("filters")

    val list =
      authenticator
        .secureEndpoint(base)
        .get
        .out(streamBody(Fs2Streams[F])(Schema.binary, CodecFormat.Json(), Some(StandardCharsets.UTF_8)))
        .serverLogicSuccess { _ => _ =>
          filterService.getAll
            .through(wrapInArray)
            .pure[F]
        }

    val find =
      authenticator
        .secureEndpoint(base)
        .get
        .in(path[FilterId])
        .out(jsonBody[Filter])
        .serverLogic { _ => filterId =>
          Logger[F].debug(s"$filterId") *> filterService
            .get(filterId)
            .map(Either.fromOption(_, ResponseError.NotFound(s"resource $filterId is not found")))
        }

    val fetch =
      authenticator
        .secureEndpoint(base)
        .get
        .in(path[FilterId])
        .in("data")
        .in(query[Option[PageSize]]("size"))
        .in(query[Option[PageNo]]("page"))
        .in(query[Option[SortBy]]("sortBy"))
        .out(streamBody(Fs2Streams[F])(Schema.binary, CodecFormat.Json(), Some(StandardCharsets.UTF_8)))
        .serverLogicSuccess { _ =>
          { case (filterId, pageSize, pageNo, sortBy) =>
            (Page.fromParamsOrDefault(pageNo, pageSize), sortBy)
              .pure[F]
              .map { case (page, sortBy) =>
                filterService
                  .getData(filterId, page, sortBy)
                  .through(wrapInArray)

              }
          }
        }

    val fetchTest =
      authenticator
        .secureEndpoint(base)
        .get
        .in(path[FilterId])
        .in("data")
        .in(query[Option[PageSize]]("size"))
        .in(query[Option[PageNo]]("page"))
        .in(query[Option[SortBy]]("sortBy"))
        .out(stringBody)
        .serverLogicSuccess { _ =>
          { case (filterId, pageSize, pageNo, sortBy) =>
            s" RESponse = $filterId, $pageSize, $pageNo, $sortBy".pure[F]
          }
        }

    val create =
      authenticator
        .secureEndpoint(base)
        .post
        .in(jsonBody[NewFilter])
        .out(jsonBody[Filter].and(statusCode(StatusCode.Created)))
        .serverLogicSuccess { _ => newFilter => filterService.create(newFilter) }

    val defaultServerOptions = Http4sServerOptions.default[F]

    def routes: HttpRoutes[F] = toRoutes("filters")(fetch, find, create, list)

  }

  def routes = Router(
    "/" -> endpoints.routes
  )
}

object FilterRoutes {
  def make[F[_]: Async: Logger](
      authenticator: Authenticator[F],
      filterService: FilterService[F]
  ) = new FilterRoutes(authenticator, filterService)
}
