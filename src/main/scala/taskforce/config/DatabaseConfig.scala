package taskforce.config

import eu.timepit.refined.string._
import eu.timepit.refined.types.string.NonEmptyString
import pureconfig.ConfigReader
import eu.timepit.refined.auto._
import eu.timepit.refined.pureconfig._
import pureconfig._
import pureconfig.generic.semiauto._
import eu.timepit.refined.api.Refined

final case class DatabaseConfig(
    driver: NonEmptyString,
    url: String Refined Uri,
    user: NonEmptyString,
    pass: NonEmptyString
)

object DatabaseConfig {

  implicit val configReader: ConfigReader[DatabaseConfig] =
    deriveReader[DatabaseConfig]

}
