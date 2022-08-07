package taskforce.filter

import monix.newtypes.NewtypeWrapped
import java.util.UUID

package object model {

  type FilterId = FilterId.Type
  object FilterId extends NewtypeWrapped[UUID]
}
