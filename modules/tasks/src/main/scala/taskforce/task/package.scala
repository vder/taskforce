package taskforce

import monix.newtypes.NewtypeWrapped
import eu.timepit.refined.types.numeric.PosInt
import java.util.UUID
import java.time.Duration
import eu.timepit.refined.types.string.NonEmptyString

package object task {

  type ProjectId = ProjectId.Type
  object ProjectId extends NewtypeWrapped[Long]

  type TaskId = TaskId.Type
  object TaskId extends NewtypeWrapped[UUID]

  type TaskDuration = TaskDuration.Type
  object TaskDuration extends NewtypeWrapped[Duration]

  type TaskVolume = TaskVolume.Type
  object TaskVolume extends NewtypeWrapped[PosInt]

  type TaskComment = TaskComment.Type
  object TaskComment extends NewtypeWrapped[NonEmptyString]

}
