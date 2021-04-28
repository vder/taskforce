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

final case class TaskId(value: String) extends ResourceId[String]

object TaskId {
  implicit val taskIdDecoder: Decoder[TaskId] =
    Decoder[String].map(TaskId.apply)
  implicit val taskIdEncoder: Encoder[TaskId] =
    Encoder[String].contramap(_.value)
}

final case class TaskDTO(
    id: Long,
    projectId: ProjectId,
    owner: UUID,
    created: LocalDateTime,
    duration: String,
    volume: Int,
    deleted: Option[LocalDateTime],
    comment: Option[String]
)

final case class NewTaskDTO(
    created: Option[LocalDateTime],
    projectId: Long,
    duration: Long,
    volume: Option[Int],
    comment: Option[String]
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

  val gen = PrettyIdGenerator.singleNode

  def fromNewTask(
      newTask: NewTaskDTO,
      userId: UserId,
      projectId: ProjectId
  ) =
    (for {
      volume <- newTask.volume match {
        case None    => None.asRight[String]
        case Some(x) => refineV[Positive](x).map(_.some)
      }
      comment <- newTask.comment match {
        case None                   => None.asRight[String]
        case Some(s) if s.isEmpty() => None.asRight[String]
        case Some(s)                => refineV[NonEmpty](s).map(_.some)
      }
    } yield Task(
      TaskId(gen.nextId()),
      ProjectId(newTask.projectId),
      userId,
      newTask.created.getOrElse(LocalDateTime.now()),
      TaskDuration(Duration.ofMinutes(newTask.duration)),
      volume,
      None,
      comment
    )).leftMap(TaskCreationError(_))

  implicit val taskDecoder: Decoder[Task] =
    deriveDecoder[Task]
  implicit val taskEncoder: Encoder[Task] =
    deriveEncoder[Task]
}

object NewTaskDTO {

  implicit val newTaskDtoDecoder: Decoder[NewTaskDTO] =
    deriveDecoder[NewTaskDTO]
  implicit val newTaskDtoEncoder: Encoder[NewTaskDTO] =
    deriveEncoder[NewTaskDTO]
}
