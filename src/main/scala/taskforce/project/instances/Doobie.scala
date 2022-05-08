package taskforce.project.instances

import doobie.Meta
import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string
import io.getquill.MappedEncoding
import taskforce.project.TotalTime

import java.time.Duration


trait Doobie {

  implicit val totalTimeMeta: Meta[TotalTime] =
    Meta[Long].imap(x => TotalTime(Duration.ofMinutes(x)))(x =>
      x.value.toMinutes
    )

  implicit val decodeTotalTime: MappedEncoding[Long, TotalTime] =
    MappedEncoding[Long, TotalTime](long =>
      TotalTime(Duration.ofMinutes(long))
    )
  implicit val encodeTotalTime: MappedEncoding[TotalTime, Long] =
    MappedEncoding[TotalTime, Long](_.value.toMinutes)

  implicit val decodeNonEmptyString: MappedEncoding[String, string.NonEmptyString] = MappedEncoding[String, string.NonEmptyString](Refined.unsafeApply)
  implicit val encodeNonEmptyString: MappedEncoding[string.NonEmptyString, String] = MappedEncoding[string.NonEmptyString, String](_.value)
  implicit val taskDurationNumeric: Numeric[TotalTime]  = fakeNumeric[TotalTime]

  def fakeNumeric[T]: Numeric[T] = new Numeric[T] {

    override def compare(x: T, y: T): Int = ???

    override def plus(x: T, y: T): T = ???

    override def minus(x: T, y: T): T = ???

    override def times(x: T, y: T): T = ???

    override def negate(x: T): T = ???

    override def fromInt(x: Int): T = ???

    override def parseString(str: String): Option[T] = ???

    override def toInt(x: T): Int = ???

    override def toLong(x: T): Long = ???

    override def toFloat(x: T): Float = ???

    override def toDouble(x: T): Double = ???

  }
}
