package taskforce.common.instances

import org.http4s.EntityEncoder
import org.http4s.circe._
import taskforce.common.ErrorMessage


trait Http4s[F[_]] extends Circe {

  implicit val errorMessageEntityEncoder: EntityEncoder[F, ErrorMessage]            = jsonEncoderOf[F, ErrorMessage]
}
