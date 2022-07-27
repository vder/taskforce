package taskforce.task.instances

import io.circe._, io.circe.generic.semiauto._
import io.circe.refined._
import monix.newtypes.integrations.DerivedCirceCodec
import taskforce.task.Task
import taskforce.task.NewTask
import taskforce.task.TaskDuration
import java.time.Duration

trait Circe extends DerivedCirceCodec {

  implicit val taskDecoder: Decoder[Task] =
    deriveDecoder
  implicit val taskEncoder: Encoder[Task] =
    deriveEncoder

  implicit val newTaskDecoder: Decoder[NewTask] =
    deriveDecoder
  implicit val newTaskEncoder: Encoder[NewTask] =
    deriveEncoder

  implicit val taskDurationEncoder: Encoder[TaskDuration] =
    Encoder[Long].contramap(_.value.toMinutes())

  implicit val taskDurationDecoder: Decoder[TaskDuration] =
    Decoder[Long].map(x => TaskDuration(Duration.ofMinutes(x)))

}
