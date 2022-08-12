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
    Codec.string.mapDecode {
      case "created" =>
        DecodeResult.Value(SortBy(Field.CreatedDate, Order.Asc))
      case "updated" =>
        DecodeResult.Value(SortBy(Field.UpdatedDate, Order.Asc))
      case "-created" => DecodeResult.Value(SortBy(Field.CreatedDate, Order.Desc))
      case "-updated" => DecodeResult.Value(SortBy(Field.UpdatedDate, Order.Desc))
      case s => DecodeResult.Error(s, AppError.InvalidQueryParam(s"SortBy=$s"))
    }(sortBy =>
      sortBy match {
        case SortBy(Field.CreatedDate, Order.Desc) => "-created"
        case SortBy(Field.CreatedDate, Order.Asc)  => "created"
        case SortBy(Field.UpdatedDate, Order.Desc) => "-updated"
        case SortBy(Field.UpdatedDate, Order.Asc)  => "updated"
      }
    )
}
