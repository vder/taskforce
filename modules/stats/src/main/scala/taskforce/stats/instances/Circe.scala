package taskforce.stats.instances

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import taskforce.stats.{StatsResponse, StatsQuery}
import monix.newtypes.integrations.DerivedCirceCodec

trait Circe extends DerivedCirceCodec {

  implicit val circeStatsResponseDecoder: Decoder[StatsResponse] =
    deriveDecoder[StatsResponse]
  implicit val circeStatsResponseEncoder: Encoder[StatsResponse] =
    deriveEncoder[StatsResponse]

  implicit val circeStatsQueryDecoder: Decoder[StatsQuery] = deriveDecoder

  implicit val circeStatsQueryEncoder: Encoder[StatsQuery] = deriveEncoder

}
