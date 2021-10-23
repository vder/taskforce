package taskforce.filter

import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import java.util.UUID
import taskforce.common.{errors => commonErrors}
final class FilterService[F[_]: Sync ](
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
  def make[F[_]: Sync](filterRepo: FilterRepository[F]) =
    Sync[F].delay(
      new FilterService[F](filterRepo)
    )
}
