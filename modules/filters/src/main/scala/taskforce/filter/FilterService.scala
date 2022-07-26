package taskforce.filter

import cats.implicits._
import fs2.Stream
import java.util.UUID
import taskforce.common.{errors => commonErrors}
import cats.MonadThrow
final class FilterService[F[_]: MonadThrow](
    filterRepo: FilterRepository[F]
) {

  def create(newFilter: NewFilter) =
    filterRepo
      .create(
        Filter(FilterId(UUID.randomUUID()), newFilter.conditions)
      )

  def getAll                  = filterRepo.list
  def get(filterId: FilterId) = filterRepo.find(filterId)
  def getData(filterId: FilterId, pagination: Page, sortBy: Option[SortBy]) =
    for {
      filterOption <- Stream.eval(
        filterRepo
          .find(filterId)
          .ensure(commonErrors.NotFound(filterId.value.toString))(_.isDefined)
      )
      rows <- filterRepo.execute(filterOption.get, sortBy, pagination)
    } yield rows

}

object FilterService {
  def make[F[_]: MonadThrow](filterRepo: FilterRepository[F]) =
      new FilterService[F](filterRepo)
    
}
