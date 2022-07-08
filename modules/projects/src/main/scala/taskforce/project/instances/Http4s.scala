package taskforce.project.instances

import org.http4s.EntityEncoder
import org.http4s.circe._
import taskforce.project.{Project, TotalTime}

trait Http4s[F[_]] {

  implicit val totalTimeEntityEncoder: EntityEncoder[F, TotalTime]            = jsonEncoderOf[F, TotalTime]
  implicit val projectEntityEncoder: EntityEncoder[F, Project]                = jsonEncoderOf[F, Project]
  implicit val ProjectListEntityEncoder: EntityEncoder[F, List[Project]]      = jsonEncoderOf[F, List[Project]]
  implicit val ProjectOptionsEntityEncoder: EntityEncoder[F, Option[Project]] = jsonEncoderOf[F, Option[Project]]
}
