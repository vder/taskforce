package taskforce.stats

import java.time.LocalDate
import taskforce.authentication.UserId

final case class StatsQuery(
    users: List[UserId],
    from: Option[LocalDate],
    to: Option[LocalDate]
)

final case class StatsResponse(
    tasksNo: Option[Int],
    averageTaskTime: Option[Double],
    averageVolume: Option[Double],
    averageTimeVolume: Option[Double]
)
