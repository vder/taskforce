package taskforce.stats


import taskforce.authentication.UserId
import java.time.Instant

final case class StatsQuery(users: List[UserId], from: Option[Instant], to: Option[Instant])

final case class StatsResponse(
    tasksNo: Option[Int],
    averageTaskTime: Option[Double],
    averageVolume: Option[Double],
    averageTimeVolume: Option[Double]
)
