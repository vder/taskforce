package taskforce


import monix.newtypes.NewtypeWrapped
import java.time.Duration
import eu.timepit.refined.types.string.NonEmptyString

package object project {
  type ProjectId = ProjectId.Type
  object ProjectId extends NewtypeWrapped[Long]

  type ProjectName = ProjectName.Type
  object ProjectName extends NewtypeWrapped[NonEmptyString]

  type TotalTime = TotalTime.Type
  object TotalTime extends NewtypeWrapped[Duration]

}
