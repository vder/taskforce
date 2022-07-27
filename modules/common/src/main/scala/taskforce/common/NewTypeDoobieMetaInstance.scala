package taskforce.common

import cats.implicits._
import doobie.util.Put
import monix.newtypes.HasBuilder
import monix.newtypes.HasExtractor
import doobie.util.Get
import cats.Show
import io.getquill.MappedEncoding

trait NewTypeDoobieMeta extends NewTypeDoobiePut  with NewTypeDoobieGet

trait NewTypeDoobiePut {
  implicit def putInstance[T, S](implicit
      extractor: HasExtractor.Aux[T, S],
      put: Put[S]
  ): Put[T] =
    put.contramap(t => extractor.extract(t))
}

trait NewTypeDoobieGet {
  implicit def getInstance[T, S](implicit
      builder: HasBuilder.Aux[T, S],
      get: Get[S],
      sh: Show[S]
  ): Get[T] = {
    get.temap(s => builder.build(s).leftMap(_.toReadableString))
  }
}

trait NewTypeQuillInstances {

  implicit def decode[S, T](implicit
      builder: HasBuilder.Aux[T, S]
  ): MappedEncoding[S, T] = MappedEncoding[S, T](builder.build(_).toOption.get)

  implicit def encode[T, S](implicit
      extractor: HasExtractor.Aux[T, S]
  ): MappedEncoding[T, S] = MappedEncoding[T, S](extractor.extract _)

}
