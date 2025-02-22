package taskforce.filter

import org.scalacheck.Arbitrary
import generators._
import taskforce.filter.model.Criteria._
import taskforce.filter.model._

import java.time.Instant

object arbitraries {

  implicit def arbNonEmptyStringGen: Arbitrary[String] = Arbitrary(nonEmptyStringGen)
  implicit def arbInstantGen: Arbitrary[Instant]        = Arbitrary(instantGen)
  implicit def arbOperatorGen: Arbitrary[Operator]       = Arbitrary(operatorGen)
  implicit def arbStatusGen: Arbitrary[Status]         = Arbitrary(statusGen)
  implicit def arbInGen: Arbitrary[In]             = Arbitrary(inGen)
  implicit def arbStateGen: Arbitrary[State]          = Arbitrary(stateGen)
  implicit def arbConditionsGen: Arbitrary[List[Criteria]]     = Arbitrary(conditionsGen)
  implicit def arbFilterIdGen: Arbitrary[FilterId]       = Arbitrary(filterIdGen)
  implicit def arbNewFilterGen: Arbitrary[NewFilter]      = Arbitrary(newFilterGen)
  implicit def arbFilterGen: Arbitrary[Filter]         = Arbitrary(filterGen)
  implicit def arbPageSizeGen: Arbitrary[PageSize]       = Arbitrary(pageSizeGen)
  implicit def arbPageGen: Arbitrary[Page]           = Arbitrary(pageGen)
  implicit def arbSortBy: Arbitrary[SortBy]            = Arbitrary(sortByGen)
  implicit def arbRow: Arbitrary[FilterResultRow]               = Arbitrary(rowGen)

}
