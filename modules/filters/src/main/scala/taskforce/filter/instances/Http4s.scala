package taskforce.filter.instances

import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import org.http4s.{ParseFailure, QueryParamDecoder}
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import taskforce.filter.model._

trait Http4s {

  object SortByMatcher extends OptionalQueryParamDecoderMatcher[SortBy]("sortBy")

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

  object PageSizeMatcher extends OptionalQueryParamDecoderMatcher[PageSize]("size")

  implicit val pageSizeParamDecoder: QueryParamDecoder[PageSize] =
    QueryParamDecoder[Int].emap(i => refineV[Positive](i).leftMap(s => ParseFailure(s, "")).map(PageSize.apply))

  object PageNoMatcher extends OptionalQueryParamDecoderMatcher[PageNo]("page")
  implicit val pageNoParamDecoder: QueryParamDecoder[PageNo] =
    QueryParamDecoder[Int].emap(i => refineV[Positive](i).leftMap(s => ParseFailure(s, "")).map(PageNo.apply))

}
