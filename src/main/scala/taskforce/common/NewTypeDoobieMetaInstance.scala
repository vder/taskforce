package taskforce.common

import cats.implicits._
import doobie.util.Put
import monix.newtypes.HasBuilder
import monix.newtypes.HasExtractor
import doobie.util.Get
import cats.Show

trait DerivedDoobieMeta extends DerivedDoobiePut with DerivedDoobieGet

trait DerivedDoobiePut {
  implicit def putInstance[T, S](implicit
      extractor: HasExtractor.Aux[T, S],
      put: Put[S]
  ): Put[T] =
    put.contramap(t => extractor.extract(t))
}

trait DerivedDoobieGet {
  implicit def getInstance[T, S](implicit
      builder: HasBuilder.Aux[T, S],
      get: Get[S],
      sh: Show[S]
  ): Get[T] = {
    get.temap(s => builder.build(s).leftMap(_.toReadableString))
  }
}
