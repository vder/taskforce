package taskforce.repos

import taskforce.model._
import cats.implicits._
import cats.effect.IO
import fs2.Stream

case class TestFilterRepository(filters: List[Filter], rows: List[Row]) extends FilterRepository[IO] {

  override def getRows(filter: Filter, sortByOption: Option[SortBy], page: Page): Stream[IO, Row] =
    Stream.emits(rows)

  override def createFilter(filter: Filter): IO[Filter] = filter.pure[IO]

  override def deleteFilter(id: FilterId): IO[Int] = 1.pure[IO]

  override def getFilter(id: FilterId): IO[Option[Filter]] =
    filters.find(f => f.id == id).pure[IO]

  override def getAllFilters: Stream[IO, Filter] = Stream.emits(filters)
}
