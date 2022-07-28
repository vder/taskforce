package taskforce.filter

import org.scalacheck.Arbitrary
import generators._

object arbitraries {

  implicit def arbNonEmptyStringGen = Arbitrary(nonEmptyStringGen)
  implicit def arbInstantGen        = Arbitrary(instantGen)
  implicit def arbOperatorGen       = Arbitrary(operatorGen)
  implicit def arbStatusGen         = Arbitrary(statusGen)
  implicit def arbInGen             = Arbitrary(inGen)
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
