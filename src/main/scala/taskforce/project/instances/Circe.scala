package taskforce.project.instances

import io.circe.generic.semiauto._
import io.circe.refined._
import io.circe.{Decoder, Encoder}
import taskforce.authentication.instances
import taskforce.project.{NewProject, Project, ProjectId, TotalTime}

trait Circe extends instances.Circe {

  implicit val projectIdDecoder: Decoder[ProjectId] =
    Decoder[Long].map(ProjectId.apply)
  implicit val projectIdEncoder: Encoder[ProjectId] =
    Encoder[Long].contramap(_.value)

  implicit val ProjectDecoder: Decoder[Project] =
    deriveDecoder[Project]
  implicit val ProjectEncoder: Encoder[Project] =
    deriveEncoder[Project]

  implicit val NewProjectDecoder: Decoder[NewProject] =
    deriveDecoder[NewProject]
  implicit val NewProjectEncoder: Encoder[NewProject] =
    deriveEncoder[NewProject]

  implicit val totalTimeDurationEncoder: Encoder[TotalTime] =
    Encoder.forProduct1("totalTime")(tt => tt.value.toMinutes())

}
