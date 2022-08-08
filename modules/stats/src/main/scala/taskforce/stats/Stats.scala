package taskforce.stats

import taskforce.authentication.UserId


final case class StatsQuery(users: List[UserId], from: Option[DateFrom], to: Option[DateTo])

final case class StatsResponse(
    tasksNo: Option[Int],
    averageTaskTime: Option[Double],
    averageVolume: Option[Double],
    averageTimeVolume: Option[Double]
)
