package taskforce.model

import scala.util.control.NoStackTrace
import taskforce.model.domain._
import eu.timepit.refined.types.string

object errors {

  trait AppError extends NoStackTrace

  case class NotAuthorError(userId: UserId) extends AppError
  case object BadRequestError extends AppError
  case class DuplicateNameError(name: string.NonEmptyString) extends AppError

}
