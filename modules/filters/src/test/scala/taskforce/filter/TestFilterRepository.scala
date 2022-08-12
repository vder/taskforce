package taskforce.filter

import cats.implicits._
import fs2.Stream
import taskforce.filter.model._
import cats.Applicative

case class TestFilterRepository[F[_]: Applicative](filters: List[Filter], rows: List[FilterResultRow]) extends FilterRepository[F] {

  override def execute(filter: Filter, sortByOption: Option[SortBy], page: Page): Stream[F, FilterResultRow] =
    Stream.emits(rows)

  override def create(filter: Filter): F[Filter] = filter.pure[F]

  override def delete(id: FilterId): F[Int] = 1.pure[F]

  override def find(id: FilterId): F[Option[Filter]] =
    filters.find(f => f.id == id).pure[F]

  override def list: Stream[F, Filter] = Stream.emits(filters)
}
