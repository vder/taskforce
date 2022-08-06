package taskforce.stats.instances

import sttp.tapir.Schema
import taskforce.common.instances.{TapirCodecs => CommonTapirCodecs}
import taskforce.stats.StatsQuery
import taskforce.stats.StatsResponse

trait TapirCodecs extends CommonTapirCodecs {

  implicit val statsQuerySchema: Schema[StatsQuery]       = Schema.derived
  implicit val statsResponseSchema: Schema[StatsResponse] = Schema.derived

}
