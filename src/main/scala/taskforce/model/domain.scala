package taskforce.model

import java.util.UUID
import doobie.util.meta.Meta
import io.circe.Decoder
import io.circe.Encoder
import io.circe.refined._
import io.circe.generic.semiauto._
import eu.timepit.refined.types.string.NonEmptyString
import java.time.LocalDateTime
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._
import java.time.Duration
import eu.timepit.refined._
import eu.timepit.refined.collection._
import com.softwaremill.id.pretty.PrettyIdGenerator
import cats.implicits._
import taskforce.model.errors.TaskCreationError
import doobie._, doobie.implicits._
object domain {
  final case class UserId(id: UUID) extends AnyVal

  object UserId {
    implicit val userIdDecoder: Decoder[UserId] =
      Decoder[UUID].map(UserId.apply)
    implicit val userIdEncoder: Encoder[UserId] =
      Encoder[UUID].contramap(_.id)
  }

  final case class User(id: UserId)

  trait ResourceId[A] {
    val value: A
  }

  object User {
    implicit val ProjectDecoder: Decoder[User] =
      deriveDecoder[User]
    implicit val ProjectEncoder: Encoder[User] =
      deriveEncoder[User]
  }

  final case class NewProject(name: NonEmptyString)

  final case class Project(
      id: ProjectId,
      name: NonEmptyString,
      owner: UserId,
      created: LocalDateTime,
      deleted: Option[LocalDateTime]
  )

  object Project {
    implicit val ProjectDecoder: Decoder[Project] =
      deriveDecoder[Project]
    implicit val ProjectEncoder: Encoder[Project] =
      deriveEncoder[Project]
  }

  object NewProject {
    implicit val NewProjectDecoder: Decoder[NewProject] =
      deriveDecoder[NewProject]
    implicit val NewProjectEncoder: Encoder[NewProject] =
      deriveEncoder[NewProject]
  }

  final case class TaskId(value: String) extends ResourceId[String]

  object TaskId {
    implicit val taskIdDecoder: Decoder[TaskId] =
      Decoder[String].map(TaskId.apply)
    implicit val taskIdEncoder: Encoder[TaskId] =
      Encoder[String].contramap(_.value)
  }

  final case class ProjectId(value: Long) extends ResourceId[Long]

  object ProjectId {
    implicit val taskIdDecoder: Decoder[ProjectId] =
      Decoder[Long].map(ProjectId.apply)
    implicit val taskIdEncoder: Encoder[ProjectId] =
      Encoder[Long].contramap(_.value)
    implicit val taskDurationMeta: Meta[ProjectId] =
      Meta[Long].imap(ProjectId.apply)(_.value)
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

}
