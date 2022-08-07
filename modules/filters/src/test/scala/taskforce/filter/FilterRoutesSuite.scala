package taskforce.filter

import arbitraries._
import cats.effect.IO
import cats.implicits._
import fs2.Stream
import java.util.UUID
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import org.http4s.Method._
import org.scalacheck.effect.PropF
import org.typelevel.log4cats.slf4j.Slf4jLogger
import taskforce.authentication.UserId
import taskforce.common.HttpTestSuite
import taskforce.common.instances.Http4s
import taskforce.common.AppError
import taskforce.filter.model.{Status => _, _}
import sttp.tapir.Endpoint
import sttp.tapir.server.PartialServerEndpoint
import taskforce.authentication.Authenticator
import taskforce.common.ResponseError
import org.http4s.headers.Authorization

class FilterRoutesSuite extends HttpTestSuite with instances.Circe with Http4s[IO] {

  implicit def entityDecodeNewFilter: EntityDecoder[IO, NewFilter] = jsonOf
  implicit def entityEncodeNewFilter: EntityEncoder[IO, NewFilter] = jsonEncoderOf
  implicit def decodeRow: EntityDecoder[IO, FilterResultRow]       = jsonOf
  implicit def encodeRow: EntityEncoder[IO, FilterResultRow]       = jsonEncoderOf

  def testAuthenticator(userId: UserId) = new Authenticator[IO] {
    def secureEndpoints[SECURITY_INPUT, INPUT, OUTPUT](
        endpoints: Endpoint[String, INPUT, ResponseError, OUTPUT, Any]
    ): PartialServerEndpoint[String, UserId, INPUT, ResponseError, OUTPUT, Any, IO] =
      endpoints
        .serverSecurityLogic { _ => userId.asRight[ResponseError].pure[IO] }
  }

  implicit def unsafeLogger = Slf4jLogger.getLogger[IO]

  val authHeader = Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame"))

  def sortBytoQuery(s: SortBy): String =
    s"""sortBy=${if (s.order == Desc) "-" else ""}${if (s.field == CreatedDate) "created" else "updated"}"""

  def pageToQuery(p: Page) = s"page=${p.no.value.value}&size=${p.size.value.value}"

  val uri: Uri = uri"api/v1/filters"

  test("create filter") {
    PropF.forAllF { (f: NewFilter, fId: FilterId) =>
      val filterRepo = new TestFilterRepository(List(Filter(fId, f.conditions)), List())

      val routes = FilterRoutes
        .make[IO](testAuthenticator(UserId(UUID.randomUUID())), FilterService.make(filterRepo))
        .routes

      POST(f, uri, authHeader)
        .pure[IO]
        .flatMap { _ =>
          assertHttpStatus(routes, POST(f, uri, authHeader))(
            Status.Created
          )
        }
    }
  }

  test("get filter that does not exist") {
    PropF.forAllF { (fId: FilterId) =>
      val filterRepo = new TestFilterRepository(List(), List())
      val routes =
        FilterRoutes
          .make[IO](testAuthenticator(UserId(UUID.randomUUID())), FilterService.make(filterRepo))
          .routes

      GET(uri / fId.toString, authHeader)
        .pure[IO]
        .flatMap { req =>
          assertHttp(routes, req)(
            Status.NotFound,
            ResponseError.NotFound(s"resource ${fId} is not found")
          )
        }
    }
  }

  test("validate query Param decoding") {
    PropF.forAllF { (f: NewFilter, fId: FilterId, sortBy: SortBy, pg: Page, row: FilterResultRow) =>
      val queryParams = s"${pageToQuery(pg)}&${sortBytoQuery(sortBy)}"
      val filterRepo = new TestFilterRepository(List(Filter(fId, f.conditions)), List(row,row)) {
        override def execute(filter: Filter, sortByOption: Option[SortBy], page: Page): Stream[IO, FilterResultRow] = {
          if (page == pg && sortBy.some == sortByOption)
            Stream.emits(rows)
          else
            Stream.raiseError[IO](AppError.InvalidQueryParam(queryParams))
        }
      }

      val routes =
        FilterRoutes.make[IO](testAuthenticator(UserId(UUID.randomUUID())), FilterService.make(filterRepo)).routes

      GET(f, Uri.unsafeFromString(s"api/v1/filters/${fId.toString()}/data?${queryParams}"), authHeader)
        .pure[IO]
        .flatMap { req =>
          println(s"REQ: $req :: $sortBy :: $pg")
          assertHttp(routes, req)(
            Status.Ok,
            List(row)
          )
        }
    }
  }

}
