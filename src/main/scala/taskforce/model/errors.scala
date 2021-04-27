package taskforce.model

import scala.util.control.NoStackTrace
import taskforce.model.domain._
import eu.timepit.refined.types.string

object errors {

  trait AppError extends NoStackTrace

  case class NotAuthorError(userId: UserId) extends AppError
  case object BadRequestError extends AppError
  case class DuplicateNameError(name: String) extends AppError
  case class TaskCreationError(name: String) extends AppError
  case object WrongPeriodError extends AppError
  case class InvalidTask(projectId: ProjectId, taskId: TaskId) extends AppError
  case class NotFoundError[A](resourceId: ResourceId[A]) extends AppError

}
