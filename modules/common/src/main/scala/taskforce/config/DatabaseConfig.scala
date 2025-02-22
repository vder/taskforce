package taskforce.config

import eu.timepit.refined.api.Refined
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.string._
import eu.timepit.refined.types.string.NonEmptyString
import pureconfig.ConfigReader

final case class DatabaseConfig(
    driver: NonEmptyString,
    url: String Refined Uri,
    user: NonEmptyString,
    pass: NonEmptyString
)  derives ConfigReader
