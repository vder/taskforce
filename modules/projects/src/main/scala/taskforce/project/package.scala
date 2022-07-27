package taskforce

import monix.newtypes.integrations.DerivedCirceCodec
import monix.newtypes.NewtypeWrapped
import java.time.Duration
import taskforce.common.NewTypeDoobieMeta
import taskforce.common.NewTypeQuillInstances
import eu.timepit.refined.types.string.NonEmptyString





package object project {
  type ProjectId = ProjectId.Type
  object ProjectId
      extends NewtypeWrapped[Long]
      with DerivedCirceCodec
      with NewTypeDoobieMeta
      with NewTypeQuillInstances

  type ProjectName = ProjectName.Type
  object ProjectName
      extends NewtypeWrapped[NonEmptyString]
      with DerivedCirceCodec
      with NewTypeDoobieMeta
      with NewTypeQuillInstances


  type TotalTime = TotalTime.Type
  object TotalTime
      extends NewtypeWrapped[Duration]
      with DerivedCirceCodec
      with NewTypeDoobieMeta
      with NewTypeQuillInstances

}
