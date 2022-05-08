package taskforce.task

import org.scalacheck.Arbitrary
import generators._

object arbitraries {

  implicit def arbNonEmptyStringGen = Arbitrary(nonEmptyStringGen)
  implicit def arbProjectIdGen      = Arbitrary(projectIdGen)
  implicit def arbUserIdGen         = Arbitrary(userIdGen)
  implicit def arbTaskIdGen         = Arbitrary(taskIdGen)
  implicit def arbTaskDurationGen   = Arbitrary(taskDurationGen)
  implicit def arbLocalDateTimeGen  = Arbitrary(localDateTimeGen)
  implicit def arbTaskGen           = Arbitrary(taskGen)
  implicit def arbNewTaskGen        = Arbitrary(newTaskGen)

}
