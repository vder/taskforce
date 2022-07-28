package taskforce.project

import org.scalacheck.Arbitrary
import generators._

object arbitraries {

  implicit def arbNonEmptyStringGen = Arbitrary(nonEmptyStringGen)
  implicit def arbProjectIdGen      = Arbitrary(projectIdGen)
  implicit def arbUserIdGen         = Arbitrary(userIdGen)
  implicit def arbNewProjectGen     = Arbitrary(newProjectGen)
  implicit def arbInstantGen        = Arbitrary(instantGen)
  implicit def arbProjectGen        = Arbitrary(projectGen)

}
