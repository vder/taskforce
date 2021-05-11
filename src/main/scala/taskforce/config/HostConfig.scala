package taskforce.config

import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.string.NonEmptyString
import pureconfig.ConfigReader
import eu.timepit.refined.auto._
import eu.timepit.refined.pureconfig._
import pureconfig.generic.semiauto._

final case class HostConfig(
    port: UserPortNumber,
    secret: NonEmptyString
)

object HostConfig {

  implicit val configReader: ConfigReader[HostConfig] =
    deriveReader[HostConfig]

}
