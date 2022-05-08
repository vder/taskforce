package taskforce.project.instances

trait Doobie {

  def fakeNumeric[T] = new Numeric[T] {

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
