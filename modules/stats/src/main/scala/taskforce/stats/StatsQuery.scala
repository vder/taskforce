package taskforce.stats

import taskforce.authentication.UserId

final case class StatsQuery(users: List[UserId], from: Option[DateFrom], to: Option[DateTo])
