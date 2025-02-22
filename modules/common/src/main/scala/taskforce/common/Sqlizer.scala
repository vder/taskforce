package taskforce.common

import doobie.util.fragment.Fragment

trait Sqlizer[A]:
  extension (a: A) def toFragment: Fragment
