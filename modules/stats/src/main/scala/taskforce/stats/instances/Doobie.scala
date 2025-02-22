package taskforce.stats.instances

import cats.data.NonEmptyList
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import taskforce.common.{NewTypeDoobieMeta, Sqlizer}
import taskforce.stats.StatsQuery

trait Doobie extends NewTypeDoobieMeta {

  given Sqlizer[StatsQuery] with
    extension (sq: StatsQuery)
      def toFragment =
        NonEmptyList
          .fromList(sq.users)
          .map(x => fr" and " ++ Fragments.in(fr"t.author", x))
          .getOrElse(Fragment.empty)

}
