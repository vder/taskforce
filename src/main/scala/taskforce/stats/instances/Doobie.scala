package taskforce.stats.instances

import cats.data.NonEmptyList
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import taskforce.common.Sqlizer
import taskforce.stats.StatsQuery
import taskforce.common.NewTypeDoobieMeta

trait Doobie extends taskforce.task.instances.Doobie with NewTypeDoobieMeta {

  implicit val statsQuerySqlizer: Sqlizer[StatsQuery] =
    new Sqlizer[StatsQuery] {
      def toFragment(sq: StatsQuery) =
        NonEmptyList
          .fromList(sq.users)
          .map(x => fr" and " ++ Fragments.in(fr"t.author", x))
          .getOrElse(Fragment.empty)
    }

}
