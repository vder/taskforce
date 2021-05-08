package taskforce.model

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import scala.util.control.NoStackTrace

object errors {

  trait AppError extends NoStackTrace

  case class NotAuthorError(userId: UserId)                    extends AppError
  case object BadRequestError                                  extends AppError
  case class DuplicateNameError(name: String)                  extends AppError
  case class TaskCreationError(name: String)                   extends AppError
  case object WrongPeriodError                                 extends AppError
  case class InvalidTask(projectId: ProjectId, taskId: TaskId) extends AppError
  case class InvalidQueryParam(s: String)                      extends AppError
  case class NotFoundError(resourceId: String)                 extends AppError

  case class ErrorMessage(code: String, message: String)

  object ErrorMessage {

    implicit val ProjectDecoder: Decoder[ErrorMessage] =
      deriveDecoder[ErrorMessage]
    implicit val ProjectEncoder: Encoder[ErrorMessage] =
      deriveEncoder[ErrorMessage]
  }
}
