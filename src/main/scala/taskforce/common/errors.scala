package taskforce.common

import scala.util.control.NoStackTrace
import taskforce.authentication.UserId

object errors {

  case class NotAuthor(userId: UserId)    extends NoStackTrace
  case object BadRequest                  extends NoStackTrace
  case class NotFound(resourceId: String) extends NoStackTrace

  case class InvalidQueryParam(s: String) extends NoStackTrace

}
