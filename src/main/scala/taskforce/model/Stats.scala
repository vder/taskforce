package taskforce.model

import java.time.LocalDate
import io.circe.{Decoder, Encoder}, io.circe.generic.semiauto._
import java.time.format.DateTimeFormatter

final case class StatsQuery(users: List[UserId], from: Option[LocalDate], to: Option[LocalDate])

final case class StatsResponse(
    tasksNo: Option[Int],
    averageTaskTime: Option[Double],
    averageVolume: Option[Double],
    averageTimeVolume: Option[Double]
)

object StatsQuery {

  private val toDateFmt   = DateTimeFormatter.ofPattern("yyyy.MM.dd")
  private val fromDateFmt = DateTimeFormatter.ofPattern("yyyy.MM")

  implicit val circeStatsQueryDecoder: Decoder[StatsQuery] =
    Decoder.forProduct3[StatsQuery, List[UserId], Option[String], Option[String]]("users", "from", "to")((u, f, t) =>
      StatsQuery(
        u,
        f.map(x => LocalDate.parse(s"$x.01", toDateFmt)),
        t.map(x => LocalDate.parse(s"$x.01", toDateFmt))
      )
    )

  implicit val circeStatsQueryEncoder: Encoder[StatsQuery] =
    Encoder.forProduct3("users", "from", "to")(x =>
      (x.users, x.from.map(_.format(fromDateFmt)), x.to.map(_.format(fromDateFmt)))
    )

}

object StatsResponse {
  implicit val circeStatsResponseDecoder: Decoder[StatsResponse] =
    deriveDecoder[StatsResponse]
  implicit val circeStatsResponseEncoder: Encoder[StatsResponse] =
    deriveEncoder[StatsResponse]
}
