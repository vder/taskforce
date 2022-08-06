package taskforce.common

import scala.util.control.NoStackTrace
import java.util.UUID
import cats.implicits._
import cats.MonadThrow

sealed trait AppError extends NoStackTrace with Product with Serializable

object AppError {
  final case class NotAuthor(userId: UUID) extends AppError

  final case class NotFound(resourceId: String)              extends AppError
  final case class InvalidQueryParam(s: String)              extends AppError
  final case class DuplicateProjectName(projectName: String) extends AppError
  final case class InvalidStatsQueryParam(s: String)         extends AppError
  final case class InvalidTask(s: String)                    extends AppError
  final case class InvalidNewProject(s: String)              extends AppError
  final case object InvalidNewFilter                         extends AppError
  final case class DuplicateTaskNameError(task: String)      extends AppError
  final case object WrongPeriodError                         extends AppError

}
sealed trait ResponseError extends Product with Serializable {
  def message: String
}

object ResponseError {

  def fromAppError: PartialFunction[Throwable, ResponseError] = {
    case AppError.NotAuthor(userId)    => NotAuthor(s"user $userId is not an Author")
    case AppError.NotFound(resourceId) => NotFound(s"resource $resourceId is not Found");
    case AppError.DuplicateProjectName(projectName) =>
      DuplicateProjectName2(s"project name '$projectName' already exists")
    case AppError.InvalidQueryParam(s)      => BadRequest(s)
    case AppError.InvalidStatsQueryParam(s) => BadRequest(s)
    case AppError.InvalidTask(s)            => BadRequest(s)
    case AppError.InvalidNewProject(s)      => BadRequest(s)

  }

  final case class TokenDecoding(message: String)         extends ResponseError
  final case class NotFound(message: String)              extends ResponseError
  final case class Forbidden(message: String)             extends ResponseError
  final case class DuplicateProjectName2(message: String) extends ResponseError
  final case class NotAuthor(message: String)             extends ResponseError
  final case class BadRequest(message: String)            extends ResponseError

  implicit class ResponseErrorOps1[F[_]: MonadThrow, A](fa: F[Either[ResponseError, A]]) {
    def extractFromEffectandMerge =
      fa.recover(fromAppError.andThen(_.asLeft))
  }

  implicit class ResponseErrorOps2[F[_]: MonadThrow, A](fa: F[A]) {
    def extractFromEffect =
      fa.map(_.asRight[ResponseError]).recover(fromAppError.andThen(_.asLeft))
  }
}
