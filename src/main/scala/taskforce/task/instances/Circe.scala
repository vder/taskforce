package taskforce.task.instances

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.refined._
import java.time.Duration
import java.util.UUID
import taskforce.task.TaskDuration
import taskforce.task.Task
import taskforce.task.TaskId
import taskforce.task.NewTask

trait Circe {

  implicit val taskDecoder: Decoder[Task] =
    deriveDecoder[Task]
  implicit val taskEncoder: Encoder[Task] =
    deriveEncoder[Task]

  implicit val taskDurationEncoder: Encoder[TaskDuration] =
    Encoder[Long].contramap(_.value.toMinutes())

  implicit val taskDurationDecoder: Decoder[TaskDuration] =
    Decoder[Long].map(x => TaskDuration(Duration.ofMinutes(x)))

  implicit val taskIdDecoder: Decoder[TaskId] =
    Decoder[UUID].map(TaskId.apply)
  implicit val taskIdEncoder: Encoder[TaskId] =
    Encoder[UUID].contramap(_.value)

  implicit val newTaskDecoder: Decoder[NewTask] =
    deriveDecoder[NewTask]
  implicit val newTaskEncoder: Encoder[NewTask] =
    deriveEncoder[NewTask]

}
