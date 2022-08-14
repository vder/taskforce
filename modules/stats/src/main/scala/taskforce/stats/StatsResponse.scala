package taskforce.stats

final case class StatsResponse(
    tasksNo: Option[Int],
    averageTaskTime: Option[Double],
    averageVolume: Option[Double],
    averageTimeVolume: Option[Double]
)
