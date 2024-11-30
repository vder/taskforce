package taskforce.filter

import eu.timepit.refined._
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.numeric.PosInt

sealed trait Field
final case object CreatedDate extends Field
final case object UpdatedDate extends Field

sealed trait Order
final case object Asc  extends Order
final case object Desc extends Order

final case class SortBy(field: Field, order: Order)
case class PageNo(value: PosInt)
case class PageSize(value: PosInt)

final case class Page(no: PageNo, size: PageSize)

object Page {

  val default =
    Page(PageNo(refineMV[Positive](1)), PageSize(refineMV[Positive](100)))

  def fromParamsOrDefault(
      pageNoOption: Option[PageNo],
      pageSizeOption: Option[PageSize]
  ) =
    Page(
      pageNoOption.getOrElse(PageNo.default),
      pageSizeOption.getOrElse(PageSize.default)
    )

}

object PageNo {
  val default = PageNo(refineMV[Positive](1))
}

object PageSize {
  val default = PageSize(refineMV[Positive](100))
}
