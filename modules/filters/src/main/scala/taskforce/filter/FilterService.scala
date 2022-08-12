package taskforce.filter

import cats.implicits._
import fs2.Stream
import taskforce.common.AppError
import cats.MonadThrow
import taskforce.filter.model._
import cats.effect.std.UUIDGen

final class FilterService[F[_]: MonadThrow: UUIDGen] private (filterRepo: FilterRepository[F]) {

  def create(newFilter: NewFilter): F[Filter] =
    for {
      id <- UUIDGen.randomUUID
      filter = Filter(FilterId(id), newFilter.conditions)
      _ <- filterRepo.create(filter)
    } yield filter

  def getAll: Stream[F, Filter]                  = filterRepo.list
  def get(filterId: FilterId): F[Option[Filter]] = filterRepo.find(filterId)
  def getData(filterId: FilterId, pagination: Page, sortBy: Option[SortBy]): Stream[F, FilterResultRow] =
    for {
      filterOpt <- Stream.eval(filterRepo.find(filterId))
      filter    <- filterOpt.toRight(AppError.NotFound(filterId.value.toString)).liftTo[Stream[F, *]]
      rows      <- filterRepo.execute(filter, sortBy, pagination)
    } yield rows

}

object FilterService {
  def make[F[_]: MonadThrow: UUIDGen](filterRepo: FilterRepository[F]): FilterService[F] =
    new FilterService[F](filterRepo)

}
