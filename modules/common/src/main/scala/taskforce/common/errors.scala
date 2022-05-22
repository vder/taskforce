package taskforce.common

import scala.util.control.NoStackTrace
import java.util.UUID

object errors {

  case class NotAuthor(userId: UUID) extends NoStackTrace
  case object BadRequest extends NoStackTrace
  case class NotFound(resourceId: String) extends NoStackTrace

  case class InvalidQueryParam(s: String) extends NoStackTrace

}
