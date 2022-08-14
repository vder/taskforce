package taskforce.filter.model

import eu.timepit.refined._
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.numeric.PosInt

final case class SortBy(field: Field, order: Order)
final case class PageNo(value: PosInt)
final case class PageSize(value: PosInt)

final case class Page(no: PageNo, size: PageSize)

object Page {

  val default: Page =
    Page(PageNo(refineMV[Positive](1)), PageSize(refineMV[Positive](100)))

  def fromParamsOrDefault(pageNoOption: Option[PageNo], pageSizeOption: Option[PageSize]): Page =
    Page(
      pageNoOption.getOrElse(PageNo.default),
      pageSizeOption.getOrElse(PageSize.default)
    )

}

object PageNo {
  val default: PageNo = PageNo(refineMV[Positive](1))
}

object PageSize {
  val default: PageSize = PageSize(refineMV[Positive](100))
}
