package taskforce.model

import org.http4s.QueryParamDecoder
import cats.implicits._
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import org.http4s.ParseFailure
import doobie.util.fragment.Fragment

sealed trait Field
final case object CreatedDate extends Field
final case object UpdatedDate extends Field

sealed trait Order
final case object Asc  extends Order
final case object Desc extends Order

final case class SortBy(field: Field, order: Order) {
  def toSql =
    Fragment.const(field match {
      case CreatedDate => s"order by t.started $order"
      case UpdatedDate => s"order by coalesce(t.started,p.created) $order"
    })

  def toQuery: String =
    s"""sortBy=${if (order == Desc) "-" else ""}${if (field == CreatedDate) "created" else "updated"}"""
}

case class PageNo(value: PosInt)
case class PageSize(value: PosInt)

final case class Page(no: PageNo, size: PageSize) {
  def toSql =
    (no, size) match {
      case (PageNo(pageNo), PageSize(pageSize)) =>
        Fragment.const(
          s" limit ${pageSize.value} offset ${(pageNo.value - 1) * pageSize.value}"
        )
    }
  val toQuery = s"page=${no.value.value}&size=${size.value.value}"
}

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
  object Matcher extends OptionalQueryParamDecoderMatcher[PageNo]("page")
  implicit val pageNoParamDecoder: QueryParamDecoder[PageNo] =
    QueryParamDecoder[Int].emap(i => refineV[Positive](i).leftMap(s => ParseFailure(s, "")).map(PageNo.apply))
}

object PageSize {

  val default = PageSize(refineMV[Positive](100))
  object Matcher extends OptionalQueryParamDecoderMatcher[PageSize]("size")

  implicit val pageSizeParamDecoder: QueryParamDecoder[PageSize] =
    QueryParamDecoder[Int].emap(i => refineV[Positive](i).leftMap(s => ParseFailure(s, "")).map(PageSize.apply))
}

object SortBy {

  object Matcher extends OptionalQueryParamDecoderMatcher[SortBy]("sortBy")

  implicit val sortByParamDecoder: QueryParamDecoder[SortBy] =
    QueryParamDecoder[String].map(s =>
      s match {
        case "created" =>
          SortBy(CreatedDate, Asc)
        case "updated" =>
          SortBy(UpdatedDate, Asc)
        case "-created" => SortBy(CreatedDate, Desc)
        case "-updated" => SortBy(UpdatedDate, Desc)
      }
    )

}
