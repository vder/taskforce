package taskforce.filter.model

import java.util.UUID

final case class FilterId(value: UUID)
final case class NewFilter(conditions: List[Criteria])
final case class Filter(id: FilterId, conditions: List[Criteria])
