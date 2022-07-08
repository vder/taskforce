package taskforce

import monix.newtypes.integrations.DerivedCirceCodec
import monix.newtypes.NewtypeWrapped
import java.time.LocalDateTime

package object common {

  type CreationDate = CreationDate.Type
  object CreationDate
      extends NewtypeWrapped[LocalDateTime]
      with DerivedCirceCodec
      with NewTypeDoobieMeta
      with NewTypeQuillInstances

  type DeletionDate = DeletionDate.Type
  object DeletionDate
      extends NewtypeWrapped[LocalDateTime]
      with DerivedCirceCodec
      with NewTypeDoobieMeta
      with NewTypeQuillInstances
}
