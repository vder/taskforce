package taskforce

import java.util.UUID
import monix.newtypes._
import monix.newtypes.integrations.DerivedCirceCodec

package object authentication {
  type UserId = UserId.Type
  object UserId extends NewtypeWrapped[UUID] with DerivedCirceCodec
}
