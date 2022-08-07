package taskforce.filter.instances

import sttp.tapir.Schema
import taskforce.filter.model.Filter
import taskforce.common.instances.{TapirCodecs => CommonTapirCodecs}
import taskforce.filter.model._
import sttp.tapir.Codec
import sttp.tapir.CodecFormat
import sttp.tapir.DecodeResult
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import taskforce.common.AppError

trait TapirCodecs extends CommonTapirCodecs {

  implicit val operatorSchema: Schema[Operator] = Schema.derived
  implicit val statusSchema: Schema[Status]     = Schema.derived
  implicit val criteriaSchema: Schema[Criteria] = Schema.derived

  implicit val filterSchema: Schema[Filter]       = Schema.derived
  implicit val newFilterSchema: Schema[NewFilter] = Schema.derived

  implicit val pageSizeCodec: Codec[String, PageSize, CodecFormat.TextPlain] =
    Codec.string.mapDecode(s =>
      s.toIntOption match {
        case Some(i) if i > 0 => DecodeResult.Value(PageSize(Refined.unsafeApply[Int, Positive](i)))
        case _                => DecodeResult.Error(s, AppError.InvalidQueryParam(s"pageSize=$s"))
      }
    )(pageSize => s"${pageSize.value.value}")

  implicit val pageNoCodec: Codec[String, PageNo, CodecFormat.TextPlain] =
    Codec.string.mapDecode(s =>
      s.toIntOption match {
        case Some(i) if i > 0 => DecodeResult.Value(PageNo(Refined.unsafeApply[Int, Positive](i)))
        case _                => DecodeResult.Error(s, AppError.InvalidQueryParam(s"pageNo=$s"))
      }
    )(pageNo => s"${pageNo.value.value}")

  implicit val sortByCodec: Codec[String, SortBy, CodecFormat.TextPlain] =
    Codec.string.mapDecode(s =>
      s match {
        case "created" =>
          DecodeResult.Value(SortBy(CreatedDate, Asc))
        case "updated" =>
          DecodeResult.Value(SortBy(UpdatedDate, Asc))
        case "-created" => DecodeResult.Value(SortBy(CreatedDate, Desc))
        case "-updated" => DecodeResult.Value(SortBy(UpdatedDate, Desc))
        case _          => DecodeResult.Error(s, AppError.InvalidQueryParam(s"SortBy=$s"))
      }
    )(sortBy =>
      sortBy match {
        case SortBy(CreatedDate, Desc) => "-created"
        case SortBy(CreatedDate, Asc)  => "created"
        case SortBy(UpdatedDate, Desc) => "-updated"
        case SortBy(UpdatedDate, Asc)  => "updated"
      }
    )
}
