package taskforce.common

import doobie.util.fragment.Fragment
import simulacrum._

@typeclass trait Sqlizer[A] {
  def toFragment(a: A): Fragment
}
