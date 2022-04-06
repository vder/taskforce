package taskforce.project.instances

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
    Decoder.forProduct5(
      "id",
      "name",
      "author",
      "created",
      "deleted"
    )(Project.apply)
  implicit val ProjectEncoder: Encoder[Project] =
    Encoder.forProduct5(
      "id",
      "name",
      "author",
      "created",
      "deleted"
    )(p => (p.id, p.name, p.author, p.created, p.deleted))

  implicit val NewProjectDecoder: Decoder[NewProject] =
    Decoder.forProduct1("name")(NewProject.apply)
  implicit val NewProjectEncoder: Encoder[NewProject] =
    Encoder.forProduct1("name")(p => (p.name))

  implicit val totalTimeDurationEncoder: Encoder[TotalTime] =
    Encoder.forProduct1("totalTime")(tt => tt.value.toMinutes())

}
