package taskforce

import monix.newtypes.NewtypeWrapped
import java.time.LocalDate

package object stats {

  type DateFrom = DateFrom.Type
  object DateFrom extends NewtypeWrapped[LocalDate]

  type DateTo = DateTo.Type
  object DateTo extends NewtypeWrapped[LocalDate]

}
