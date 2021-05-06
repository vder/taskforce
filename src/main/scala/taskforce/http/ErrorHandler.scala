package taskforce.http

import cats.data.{Kleisli, OptionT}
import cats.{MonadError, ApplicativeError}
import org.http4s.HttpRoutes
import org.http4s.Response
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import taskforce.model.errors._

trait ErrorHandler[F[_], E <: Throwable] {
  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}

object LiveHttpErrorHandler {

  def apply[F[_]: MonadError[*[_], Throwable]]: ErrorHandler[F, Throwable] =
    new ErrorHandler[F, Throwable] {
      val dsl = new Http4sDsl[F] {}
      import dsl._
      val A: ApplicativeError[F, Throwable] = implicitly

      val handler: Throwable => F[Response[F]] = {
        case NotAuthorError(x) =>
          Forbidden(
            ErrorMessage("001", "User is not an owner of the resource")
          )
        case BadRequestError =>
          BadRequest(ErrorMessage("002", "Invalid request"))
        case DuplicateNameError(name) =>
          Conflict(
            ErrorMessage("003", s"name given in request: $name already exists")
          )
        case TaskCreationError(name) =>
          BadRequest(
            ErrorMessage("004", s"invalid task's properties passed in request")
          )
        case WrongPeriodError =>
          Conflict(
            ErrorMessage("005", "Reporter already logged task in a given time")
          )
        case InvalidTask(projectId, taskId) =>
          NotFound(
            ErrorMessage(
              "006",
              s"task: ${taskId.value} does not exist for project:${projectId.value}"
            )
          )
        case NotFoundError(id) =>
          NotFound(
            ErrorMessage(
              "007",
              s"resource with given id ${id.value} does not exist"
            )
          )
      }

      override def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
        Kleisli { req =>
          OptionT {
            A.handleErrorWith(routes.run(req).value)(e => A.map(handler(e))(Option(_)))
          }
        }

    }
}
