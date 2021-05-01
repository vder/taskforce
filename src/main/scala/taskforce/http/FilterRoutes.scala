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
import org.http4s.{AuthedRequest, AuthedRoutes}
import taskforce.Validations
import taskforce.model._
import taskforce.model.errors._
import taskforce.repository.{ProjectRepository, TaskRepository}
import taskforce.repository.FilterRepository
import fs2.Stream

final class FilterRoutes[
    F[_]: Sync: Applicative: MonadError[
      *[_],
      Throwable
    ]: JsonDecoder
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
      case GET -> Root / UUIDVar(
            filterId
          ) as userId =>
        val filter = filterRepo.getFilter(FilterId(filterId)).map(x => x.asJson)
        Ok(filter)
      case GET -> Root / UUIDVar(
            filterId
          ) / "data" as userId =>
        val resultMap = for {
          filterOption <- Stream.eval(filterRepo.getFilter(FilterId(filterId)))
          filter <- Stream.eval(
            Sync[F].fromOption(filterOption, NotFoundError(FilterId(filterId)))
          )
          (project, taskOpt) <- filterRepo.getRows(filter)
          projectMap = Map(
            "projectId" -> project.id.asJson,
            "projectName" -> project.name.asJson,
            "projectCreated" -> project.created.asJson
          )
          taskMap =
            taskOpt
              .map(t =>
                Map(
                  "taskComment" -> t.comment.asJson,
                  "taskCreated" -> t.created.asJson,
                  "taskDuration" -> t.duration.asJson
                )
              )
              .getOrElse(Map())
          map = projectMap ++ taskMap
        } yield map.asJson
        Ok(resultMap)
    }
  }

  val routes = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}

object FilterRoutes {
  def make[F[_]: Defer: MonadError[*[_], Throwable]: Sync](
      authMiddleware: AuthMiddleware[F, UserId],
      filterRepo: FilterRepository[F]
  ) = Sync[F].delay { new FilterRoutes(authMiddleware, filterRepo) }
}
