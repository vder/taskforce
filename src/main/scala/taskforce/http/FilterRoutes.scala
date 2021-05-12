package taskforce.http

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, Defer, MonadError}
import io.circe.syntax._
import io.circe.refined._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.AuthedRoutes
import taskforce.model._
import taskforce.model.errors._
import taskforce.repos.FilterRepository
import fs2.Stream
import java.util.UUID
import io.chrisdavenport.log4cats.Logger

final class FilterRoutes[
    F[_]: Sync: Applicative: MonadError[
      *[_],
      Throwable
    ]: JsonDecoder: Logger
](
    authMiddleware: AuthMiddleware[F, UserId],
    filterRepo: FilterRepository[F]
) {

  private[this] val prefixPath = "/api/v1/filters"

  implicit def decodeTask = jsonOf
  implicit def encodeTask = jsonEncoderOf

  val httpRoutes: AuthedRoutes[UserId, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    AuthedRoutes.of {
      case authReq @ POST -> Root as userId =>
        for {
          newFilter <-
            authReq.req
              .asJsonDecode[NewFilter]
              .adaptError(_ => BadRequestError)
          filter <-
            filterRepo
              .createFilter(
                Filter(FilterId(UUID.randomUUID()), newFilter.conditions)
              )
          response <- Created(filter.asJson)
        } yield response
      case GET -> Root as userId =>
        val result = filterRepo.getAllFilters.map(_.asJson)
        Ok(result)
      case GET -> Root / UUIDVar(
            filterId
          ) as userId =>
        val id = FilterId(filterId)
        val filter = filterRepo
          .getFilter(id)
          .ensure(NotFoundError(id.toString()))(!_.isEmpty)
        Ok(filter)
      case GET -> Root / UUIDVar(
            filterId
          ) / "data" :? SortBy.Matcher(sortBy)
          :? PageNo.Matcher(no)
          :? PageSize.Matcher(size) as userId =>
        val id = FilterId(filterId)
        val rowsStream = for {
          filterOption <- Stream.eval(
            filterRepo
              .getFilter(id)
              .ensure(NotFoundError(id.toString))(_.isDefined)
          )
          page = Page.fromParamsOrDefault(no, size)
          rows <- filterRepo.getRows(filterOption.get, sortBy, page)
        } yield rows.asJson
        Ok(rowsStream)
    }
  }

  val routes = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}

object FilterRoutes {
  def make[F[_]: Defer: MonadError[*[_], Throwable]: Sync: Logger](
      authMiddleware: AuthMiddleware[F, UserId],
      filterRepo: FilterRepository[F]
  ) = Sync[F].delay { new FilterRoutes(authMiddleware, filterRepo) }
}
