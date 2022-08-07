package taskforce.filter.model


final case class NewFilter(conditions: List[Criteria])
final case class Filter(id: FilterId, conditions: List[Criteria])
