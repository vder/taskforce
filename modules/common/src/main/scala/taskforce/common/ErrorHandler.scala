package taskforce.common

import cats.data.{Kleisli, OptionT}
import cats.{MonadError, ApplicativeError}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response}

trait ErrorHandler[F[_], E <: Throwable] {
  def handle(
      otherHandler: PartialFunction[Throwable, F[Response[F]]]
  )(routes: HttpRoutes[F]): HttpRoutes[F]

  def basicHandle(routes: HttpRoutes[F]): HttpRoutes[F] = handle(PartialFunction.empty)(routes)
}

object LiveHttpErrorHandler {

  def apply[F[_]: MonadError[*[_], Throwable]]: ErrorHandler[F, Throwable] =
    new ErrorHandler[F, Throwable] {
      val dsl = new Http4sDsl[F] {}
      import dsl._
      val A: ApplicativeError[F, Throwable] = implicitly

      val handler: PartialFunction[Throwable, F[Response[F]]] = {
        case errors.NotAuthor(_) =>
          Forbidden(
            ErrorMessage("BASIC-003", "User is not an owner of the resource")
          )
        case errors.BadRequest =>
          BadRequest(ErrorMessage("BASIC-002", "Invalid request"))

        case errors.NotFound(id) =>
          NotFound(
            ErrorMessage(
              "BASIC-001",
              s"resource with given id ${id} does not exist"
            )
          )
      }

      override def handle(
          otherHandler: PartialFunction[Throwable, F[Response[F]]]
      )(routes: HttpRoutes[F]): HttpRoutes[F] =
        Kleisli { req =>
          val finalHandler = handler orElse otherHandler
          OptionT {
            A.handleErrorWith(routes.run(req).value)(e => A.map(finalHandler(e))(Option(_)))
          }
        }

    }
}
