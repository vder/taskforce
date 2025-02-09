package taskforce.config

import com.comcast.ip4s._
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.pureconfig._
import pureconfig.ConfigReader
import pureconfig.error._
import pureconfig.generic.semiauto._

final case class HostConfig(
    port: Port,
    secret: NonEmptyString
)

object HostConfig {

  implicit val portConfigReader: ConfigReader[Port] = ConfigReader.fromString[Port] { str =>
  Port.fromString(str).toRight(CannotConvert(str, "Port", "Must be a valid port number (1-65535)"))
  }

  implicit val configReader: ConfigReader[HostConfig] =
    deriveReader
}
