package taskforce

import org.scalacheck.Arbitrary
import taskforce.generators._

object arbitraries {

  implicit def arbNonEmptyStringGen = Arbitrary(nonEmptyStringGen)
  implicit def arbUserIdGen         = Arbitrary(userIdGen)
  implicit def arbNewProjectGen     = Arbitrary(newProjectGen)
  implicit def arbLocalDateTimeGen  = Arbitrary(localDateTimeGen)
  implicit def arbOperatorGen       = Arbitrary(operatorGen)
  implicit def arbStatusGen         = Arbitrary(statusGen)
  implicit def arbInGen             = Arbitrary(inGen)
  implicit def arbTaskCreatedGen    = Arbitrary(taskCreatedGen)
  implicit def arbStateGen          = Arbitrary(stateGen)
  implicit def arbConditionsGen     = Arbitrary(conditionsGen)
  implicit def arbFilterIdGen       = Arbitrary(filterIdGen)
  implicit def arbNewFilterGen      = Arbitrary(newFilterGen)
  implicit def arbFilterGen         = Arbitrary(filterGen)
  implicit def arbPageSizeGen       = Arbitrary(pageSizeGen)
  implicit def arbPageGen           = Arbitrary(pageGen)
  implicit def arbSortBy            = Arbitrary(sortByGen)
  implicit def arbRow               = Arbitrary(rowGen)

}
