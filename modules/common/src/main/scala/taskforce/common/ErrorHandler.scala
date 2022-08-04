package taskforce.common

import cats.data.{Kleisli, OptionT}
import cats.MonadError
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response}
import cats.syntax.all._

trait ErrorHandler[F[_]] {
  def handle(
      otherHandler: PartialFunction[Throwable, F[Response[F]]]
  )(routes: HttpRoutes[F]): HttpRoutes[F]

  def basicHandle(routes: HttpRoutes[F]): HttpRoutes[F] = handle(PartialFunction.empty)(routes)
}

object ErrorHandler {

  def apply[F[_]: MonadError[*[_], Throwable]]: ErrorHandler[F] =
    new ErrorHandler[F] {
      val dsl = new Http4sDsl[F] {}
      import dsl._

      val handler: PartialFunction[Throwable, F[Response[F]]] = {
        case AppError.NotAuthor(_) =>
          Forbidden(
            ErrorMessage("BASIC-003", "User is not an owner of the resource")
          )
        case BadRequest =>
          BadRequest(ErrorMessage("BASIC-002", "Invalid request"))

        case AppError.NotFound(id) =>
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

          routes.run(req).handleErrorWith(err => OptionT.liftF(finalHandler(err)))
        }

    }
}
