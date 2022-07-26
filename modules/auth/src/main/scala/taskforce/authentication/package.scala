package taskforce

import java.util.UUID
import monix.newtypes._

package object authentication {
  type UserId = UserId.Type
  object UserId extends NewtypeWrapped[UUID]
}
