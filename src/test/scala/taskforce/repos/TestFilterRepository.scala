package taskforce.repos

import taskforce.model._
import cats.implicits._
import cats.effect.IO
import fs2.Stream

final case class TestFilterRepository(filters: List[Filter], tasks: List[(Project, List[Task])])
    extends FilterRepository[IO] {

  override def getRows(filter: Filter, sortByOption: Option[SortBy], page: Page): Stream[IO, Row] = {
    val rows = tasks.flatMap {
      case (p, taskList) if taskList.isEmpty => List(Row.fromTuple(p, None))
      case (p, taskList)                     => taskList.map(t => Row.fromTuple(p, t.some))
    }

    val result = filter.conditions.foldLeft(rows)((r, c) => r.filter(c.filter))

    Stream.emits(result)
  }

  override def createFilter(filter: Filter): IO[Filter] = filter.pure[IO]

  override def deleteFilter(id: FilterId): IO[Int] = 1.pure[IO]

  override def getFilter(id: FilterId): IO[Option[Filter]] =
    filters.find(f => f.id == id).pure[IO]

  override def getAllFilters: Stream[IO, Filter] = Stream.emits(filters)
}
