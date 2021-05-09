package taskforce.routes

import taskforce.repos.TestFilterRepository

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
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
import taskforce.http.LiveHttpErrorHandler
import taskforce.model.{Status => _, _}
import taskforce.model.errors._
import taskforce.http.FilterRoutes
import fs2.Stream
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

class FilterRoutesSuite extends HttpTestSuite {
  implicit def decodeNewFilter: EntityDecoder[IO, NewFilter] = jsonOf
  implicit def encodeNewFilter: EntityEncoder[IO, NewFilter] = jsonEncoderOf
  implicit def decodeRow: EntityDecoder[IO, Row]             = jsonOf
  implicit def encodeRow: EntityEncoder[IO, Row]             = jsonEncoderOf

  implicit def unsafeLogger = Slf4jLogger.getLogger[IO]
  val errHandler            = LiveHttpErrorHandler.apply[IO]
  def authMiddleware: AuthMiddleware[IO, UserId] =
    AuthMiddleware(Kleisli.pure(UserId(UUID.randomUUID())))

  def authMiddleware(userId: UserId): AuthMiddleware[IO, UserId] =
    AuthMiddleware(Kleisli.pure(userId))

  val uri = uri"api/v1/filters/"

  test("create filter") {
    PropF.forAllF { (f: NewFilter, fId: FilterId) =>
      val filterRepo = new TestFilterRepository(List(Filter(fId, f.conditions)), List())
      val routes     = errHandler.handle(new FilterRoutes[IO](authMiddleware, filterRepo).routes)
      POST(f, uri).flatMap { req =>
        assertHttpStatus(routes, req)(
          Status.Created
        )
      }
    }
  }
  test("get filter that does not exist") {
    PropF.forAllF { (f: NewFilter, fId: FilterId, fId2: FilterId) =>
      val filterRepo = new TestFilterRepository(List(Filter(fId2, f.conditions)), List())
      val routes     = errHandler.handle(new FilterRoutes[IO](authMiddleware, filterRepo).routes)
      GET(f, Uri.unsafeFromString(s"api/v1/filters/${fId.value}?sortBy=-creaed")).flatMap { req =>
        assertHttp(routes, req)(
          Status.NotFound,
          ErrorMessage(
            "007",
            s"resource with given id ${fId} does not exist"
          )
        )
      }
    }
  }

  test("validate query Param decoding") {
    PropF.forAllF { (f: NewFilter, fId: FilterId, sortBy: SortBy, pg: Page, row: Row) =>
      val queryParams = s"${pg.toQuery}&${sortBy.toQuery}"
      val filterRepo = new TestFilterRepository(List(Filter(fId, f.conditions)), List(row)) {
        override def getRows(filter: Filter, sortByOption: Option[SortBy], page: Page): Stream[IO, Row] = {
          if (page == pg && sortBy.some == sortByOption)
            Stream.emits(rows)
          else
            Stream.raiseError[IO](InvalidQueryParam(queryParams))
        }
      }

      val routes = errHandler.handle(new FilterRoutes[IO](authMiddleware, filterRepo).routes)
      GET(f, Uri.unsafeFromString(s"api/v1/filters/${fId.value}/data?${queryParams}")).flatMap { req =>
        assertHttp(routes, req)(
          Status.Ok,
          row
        )
      }
    }
  }

}
