package taskforce.stats.instances

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import taskforce.authentication.UserId
import taskforce.stats.{StatsResponse, StatsQuery}
import monix.newtypes.integrations.DerivedCirceCodec

trait Circe extends DerivedCirceCodec {

  private val toDateFmt   = DateTimeFormatter.ofPattern("yyyy.MM.dd")
  private val fromDateFmt = DateTimeFormatter.ofPattern("yyyy.MM")

  implicit val circeStatsResponseDecoder: Decoder[StatsResponse] =
    deriveDecoder[StatsResponse]
  implicit val circeStatsResponseEncoder: Encoder[StatsResponse] =
    deriveEncoder[StatsResponse]

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
