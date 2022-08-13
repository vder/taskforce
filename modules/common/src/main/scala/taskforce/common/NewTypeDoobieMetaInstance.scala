package taskforce.common

import cats.implicits._
import doobie.util.Put
import monix.newtypes.HasBuilder
import monix.newtypes.HasExtractor
import doobie.util.Get
import cats.Show
import io.getquill.MappedEncoding

trait NewTypeDoobieMeta extends NewTypeDoobiePut with NewTypeDoobieGet

trait NewTypeDoobiePut {
  implicit def putInstance[New, Base](implicit
      extractor: HasExtractor.Aux[New, Base],
      put: Put[Base]
  ): Put[New] =
    put.contramap(t => extractor.extract(t))
}

trait NewTypeDoobieGet {
  implicit def getInstance[New, Base](implicit
      builder: HasBuilder.Aux[New, Base],
      get: Get[Base],
      sh: Show[Base]
  ): Get[New] = {
    get.temap(s => builder.build(s).leftMap(_.toReadableString))
  }
}

trait NewTypeQuillInstances {

  implicit def decode[New, Base](implicit
      builder: HasBuilder.Aux[New, Base]
  ): MappedEncoding[Base, New] = MappedEncoding[Base, New](builder.build(_).toOption.get)

  implicit def encode[New, Base](implicit
      extractor: HasExtractor.Aux[New, Base]
  ): MappedEncoding[New, Base] = MappedEncoding[New, Base](extractor.extract _)

}
