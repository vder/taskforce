package taskforce.project.instances

import io.circe._, io.circe.generic.semiauto._
import taskforce.project.Project

import io.circe.refined._
import monix.newtypes.integrations.DerivedCirceCodec


trait Circe extends DerivedCirceCodec {

  implicit val projectDecoder: Decoder[Project] = deriveDecoder
  implicit val projectEncoder: Encoder[Project] = deriveEncoder


}
