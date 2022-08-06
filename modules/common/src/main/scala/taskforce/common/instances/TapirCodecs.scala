package taskforce.common.instances

import monix.newtypes.HasBuilder
import monix.newtypes.HasExtractor
import sttp.tapir.Schema
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.DecodeResult
import sttp.tapir.integ.cats.TapirCodecCats
import sttp.tapir.codec.refined.TapirCodecRefined


trait TapirCodecs extends TapirCodecCats with TapirCodecRefined{

  implicit def newtypesCodec[New, Base](implicit
      builder: HasBuilder.Aux[New, Base],
      extractor: HasExtractor.Aux[New, Base],
      codec: PlainCodec[Base],
      schema: Schema[New]
  ): PlainCodec[New] =
    codec
      .mapDecode { base =>
        builder.build(base) match {
          case Right(value) => DecodeResult.Value(value)
          case Left(error)  => DecodeResult.Error(error.toReadableString, error.toException)
        }
      }(extractor.extract)
      .schema(schema)

  implicit def newtypesSchema[New, Base](implicit
      builder: HasBuilder.Aux[New, Base],
      extractor: HasExtractor.Aux[New, Base],
      schema: Schema[Base]
  ): Schema[New] =
    schema.map(base => builder.build(base).toOption)(extractor.extract)


}
