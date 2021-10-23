package taskforce.common

import doobie.util.fragment.Fragment
import simulacrum._
import scala.annotation.nowarn

@nowarn @typeclass trait Sqlizer[A] {
  def toFragment(a: A): Fragment
}
