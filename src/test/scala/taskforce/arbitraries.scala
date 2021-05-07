package taskforce

import org.scalacheck.Arbitrary
import taskforce.generators._

object arbitraries {

  implicit def arbNonEmptyStringGen = Arbitrary(nonEmptyStringGen)
  implicit def arbProjectIdGen      = Arbitrary(projectIdGen)
  implicit def arbUserIdGen         = Arbitrary(userIdGen)
  implicit def arbTaskIdGen         = Arbitrary(taskIdGen)
  implicit def arbTaskDurationGen   = Arbitrary(taskDurationGen)
  implicit def arbNewProjectGen     = Arbitrary(newProjectGen)
  implicit def arbLocalDateTimeGen  = Arbitrary(localDateTimeGen)
  implicit def arbProjectGen        = Arbitrary(projectGen)
  implicit def arbTaskGen           = Arbitrary(taskGen)
  implicit def arbNewTaskGen        = Arbitrary(newTaskGen)
  implicit def arbOperatorGen       = Arbitrary(operatorGen)
  implicit def arbStatusGen         = Arbitrary(statusGen)
  implicit def arbInGen             = Arbitrary(inGen)
  implicit def arbTaskCreatedGen    = Arbitrary(taskCreatedGen)
  implicit def arbStateGen          = Arbitrary(stateGen)
  implicit def arbConditionsGen     = Arbitrary(conditionsGen)
  implicit def arbFilterIdGen       = Arbitrary(filterIdGen)
  implicit def arbNewFilterGen      = Arbitrary(newFilterGen)
  implicit def arbFilterGen         = Arbitrary(filterGen)

}
