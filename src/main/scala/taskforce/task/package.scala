package taskforce

import monix.newtypes.integrations.DerivedCirceCodec
import monix.newtypes.NewtypeWrapped
import taskforce.common.NewTypeDoobieMeta
import taskforce.common.NewTypeQuillInstances
import java.util.UUID
import java.time.Duration
import io.circe.{Encoder => JsonEncoder,Decoder => JsonDecoder}

package object task {
  type TaskId = TaskId.Type
  object TaskId
      extends NewtypeWrapped[UUID]
      with DerivedCirceCodec
      with NewTypeDoobieMeta
      with NewTypeQuillInstances

  type TaskDuration = TaskDuration.Type
  object TaskDuration
      extends NewtypeWrapped[Duration]
      with DerivedCirceCodec
      with NewTypeDoobieMeta
      with NewTypeQuillInstances

  implicit val taskDurationEncoder: JsonEncoder[TaskDuration] =
    JsonEncoder[Long].contramap(_.value.toMinutes())

  implicit val taskDurationDecoder: JsonDecoder[TaskDuration] =
    JsonDecoder[Long].map(x => TaskDuration(Duration.ofMinutes(x)))

}
