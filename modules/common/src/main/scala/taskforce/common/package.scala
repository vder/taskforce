package taskforce

import monix.newtypes.NewtypeWrapped
import java.time.Instant

package object common {

  type CreationDate = CreationDate.Type
  object CreationDate
      extends NewtypeWrapped[Instant]
      

  type DeletionDate = DeletionDate.Type
  object DeletionDate
      extends NewtypeWrapped[Instant]
}
