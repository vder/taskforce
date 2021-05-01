package taskforce.model

import cats.implicits._
import com.softwaremill.id.pretty.PrettyIdGenerator
import doobie.util.meta.Meta
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection._
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.refined._
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import taskforce.model.errors.TaskCreationError

final case class TaskId(value: UUID) extends ResourceId[UUID]

object TaskId {
  implicit val taskIdDecoder: Decoder[TaskId] =
    Decoder[UUID].map(TaskId.apply)
  implicit val taskIdEncoder: Encoder[TaskId] =
    Encoder[UUID].contramap(_.value)
}

final case class NewTask(
    created: Option[LocalDateTime],
    projectId: ProjectId,
    duration: TaskDuration,
    volume: Option[Int Refined Positive],
    comment: Option[NonEmptyString]
)

final case class TaskDuration(value: Duration) extends AnyVal

final case class Task(
    id: TaskId,
    projectId: ProjectId,
    owner: UserId,
    created: LocalDateTime,
    duration: TaskDuration,
    volume: Option[Int Refined Positive],
    deleted: Option[LocalDateTime],
    comment: Option[NonEmptyString]
)

object TaskDuration {
  implicit val taskDurationEncoder: Encoder[TaskDuration] =
    Encoder[Long].contramap(_.value.toMinutes())
  implicit val taskDurationDecoder: Decoder[TaskDuration] =
    Decoder[Long].map(x => TaskDuration(Duration.ofMinutes(x)))

  implicit val taskDurationMeta: Meta[TaskDuration] =
    Meta[Long].imap(x => TaskDuration(Duration.ofMinutes(x)))(x =>
      x.value.toMinutes()
    )
}

object Task {

  def fromNewTask(
      newTask: NewTask,
      userId: UserId,
      projectId: ProjectId
  ) =
    Task(
      TaskId(UUID.randomUUID()),
      newTask.projectId,
      userId,
      newTask.created.getOrElse(LocalDateTime.now()),
      newTask.duration,
      newTask.volume,
      None,
      newTask.comment
    )

  implicit val taskDecoder: Decoder[Task] =
    deriveDecoder[Task]
  implicit val taskEncoder: Encoder[Task] =
    deriveEncoder[Task]
}

object NewTask {

  implicit val newTaskDtoDecoder: Decoder[NewTask] =
    deriveDecoder[NewTask]
  implicit val newTaskDtoEncoder: Encoder[NewTask] =
    deriveEncoder[NewTask]
}
