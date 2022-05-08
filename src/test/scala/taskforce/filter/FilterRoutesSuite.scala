package taskforce.filter

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import fs2.Stream
import java.util.UUID
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.scalacheck.effect.PropF
import suite.HttpTestSuite
import taskforce.arbitraries._
import taskforce.authentication.UserId
import taskforce.common.{ErrorMessage, LiveHttpErrorHandler}
import taskforce.common.errors._
import org.typelevel.log4cats.slf4j.Slf4jLogger

class FilterRoutesSuite extends HttpTestSuite with instances.Circe {

  implicit def entityDecodeNewFilter: EntityDecoder[IO, NewFilter] = jsonOf
  implicit def entityEncodeNewFilter: EntityEncoder[IO, NewFilter] =
    jsonEncoderOf
  implicit def decodeRow: EntityDecoder[IO, FilterResultRow] = jsonOf
  implicit def encodeRow: EntityEncoder[IO, FilterResultRow] = jsonEncoderOf

  val errHandler = LiveHttpErrorHandler[IO]

  implicit def unsafeLogger = Slf4jLogger.getLogger[IO]

  def authMiddleware: AuthMiddleware[IO, UserId] =
    AuthMiddleware(Kleisli.pure(UserId(UUID.randomUUID())))

  def authMiddleware(userId: UserId): AuthMiddleware[IO, UserId] =
    AuthMiddleware(Kleisli.pure(userId))

  def sortBytoQuery(s: SortBy): String =
    s"""sortBy=${if (s.order == Desc) "-" else ""}${if (s.field == CreatedDate)
        "created"
      else "updated"}"""

  def pageToQuery(p: Page) =
    s"page=${p.no.value.value}&size=${p.size.value.value}"

  val uri = uri"api/v1/filters"

  test("create filter") {
    PropF.forAllF { (f: NewFilter, fId: FilterId) =>
      val filterRepo =
        new TestFilterRepository(List(Filter(fId, f.conditions)), List())
      val routes =
        new FilterRoutes[IO](authMiddleware, new FilterService(filterRepo))
          .routes(errHandler)
      POST(f, uri).pure[IO].flatMap { _ =>
        assertHttpStatus(routes, POST(f, uri))(
          Status.Created
        )
      }
    }
  }
  test("get filter that does not exist") {
    PropF.forAllF { (f: NewFilter, fId: FilterId, fId2: FilterId) =>
      val filterRepo =
        new TestFilterRepository(List(Filter(fId2, f.conditions)), List())
      val routes =
        new FilterRoutes[IO](authMiddleware, new FilterService(filterRepo))
          .routes(errHandler)
      GET(
        f,
        Uri.unsafeFromString(s"api/v1/filters/${fId.value}?sortBy=-creaed")
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.NotFound,
          ErrorMessage(
            "BASIC-001",
            s"resource with given id ${fId} does not exist"
          )
        )
      }
    }
  }

  test("validate query Param decoding") {
    PropF.forAllF {
      (
          f: NewFilter,
          fId: FilterId,
          sortBy: SortBy,
          pg: Page,
          row: FilterResultRow
      ) =>
        val queryParams = s"${pageToQuery(pg)}&${sortBytoQuery(sortBy)}"
        val filterRepo =
          new TestFilterRepository(List(Filter(fId, f.conditions)), List(row)) {
            override def execute(
                filter: Filter,
                sortByOption: Option[SortBy],
                page: Page
            ): Stream[IO, FilterResultRow] = {
              if (page == pg && sortBy.some == sortByOption)
                Stream.emits(rows)
              else
                Stream.raiseError[IO](InvalidQueryParam(queryParams))
            }
          }

        val routes =
          new FilterRoutes[IO](authMiddleware, new FilterService(filterRepo))
            .routes(errHandler)
        GET(
          f,
          Uri.unsafeFromString(
            s"api/v1/filters/${fId.value}/data?${queryParams}"
          )
        ).pure[IO].flatMap { req =>
          assertHttp(routes, req)(
            Status.Ok,
            List(row)
          )
        }
    }
  }

}
