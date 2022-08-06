package taskforce.filter

import cats.implicits._
import cats.effect.IO
import fs2.Stream
import taskforce.filter.model._

case class TestFilterRepository(filters: List[Filter], rows: List[FilterResultRow]) extends FilterRepository[IO] {

  override def execute(filter: Filter, sortByOption: Option[SortBy], page: Page): Stream[IO, FilterResultRow] =
    Stream.emits(rows)

  override def create(filter: Filter): IO[Filter] = filter.pure[IO]

  override def delete(id: FilterId): IO[Int] = 1.pure[IO]

  override def find(id: FilterId): IO[Option[Filter]] =
    filters.find(f => f.id == id).pure[IO]

  override def list: Stream[IO, Filter] = Stream.emits(filters)
}
