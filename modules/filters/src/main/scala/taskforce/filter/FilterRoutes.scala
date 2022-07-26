package taskforce.filter

import cats.implicits._
import io.circe.syntax._
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import taskforce.authentication.UserId
import taskforce.common.{ErrorHandler, errors => commonErrors}
import cats.MonadThrow

final class FilterRoutes[
    F[_]: MonadThrow: JsonDecoder
](
    authMiddleware: AuthMiddleware[F, UserId],
    filterService: FilterService[F]
) extends instances.Circe
    with instances.Http4s {

  private[this] val prefixPath = "/api/v1/filters"

  val httpRoutes: AuthedRoutes[UserId, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    AuthedRoutes.of {
      case authReq @ POST -> Root as _ =>
        for {
          newFilter <-
            authReq.req
              .asJsonDecode[NewFilter]
              .adaptError(_ => commonErrors.BadRequest)
          filter   <- filterService.create(newFilter)
          response <- Created(filter.asJson)
        } yield response

      case GET -> Root as _ =>
        val result = filterService.getAll.map(_.asJson)
        Ok(result)
      case GET -> Root / UUIDVar(
            filterId
          ) as _ =>
        val id = FilterId(filterId)
        val filter = filterService
          .get(id)
          .ensure(commonErrors.NotFound(id.toString()))(!_.isEmpty)
        Ok(filter)
      case GET -> Root / UUIDVar(
            filterId
          ) / "data" :? SortByMatcher(sortBy)
          :? PageNoMatcher(no)
          :? PageSizeMatcher(size) as _ =>
        val id         = FilterId(filterId)
        val rowsStream = filterService.getData(id, Page.fromParamsOrDefault(no, size), sortBy)
        Ok(rowsStream.map(_.asJson))
    }
  }

  def routes(handler: ErrorHandler[F, Throwable]) =
    Router(
      prefixPath -> handler.basicHandle(authMiddleware(httpRoutes))
    )
}

object FilterRoutes {
  def make[F[_]: MonadThrow: JsonDecoder](
      authMiddleware: AuthMiddleware[F, UserId],
      filterService: FilterService[F]
  ) =  new FilterRoutes(authMiddleware, filterService) 
}
