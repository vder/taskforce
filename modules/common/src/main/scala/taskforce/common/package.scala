package taskforce

import monix.newtypes.NewtypeWrapped
import java.time.LocalDateTime

package object common {

  type CreationDate = CreationDate.Type
  object CreationDate extends NewtypeWrapped[LocalDateTime]

  type DeletionDate = DeletionDate.Type
  object DeletionDate extends NewtypeWrapped[LocalDateTime]
}
