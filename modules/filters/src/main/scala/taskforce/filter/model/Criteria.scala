package taskforce.filter.model

import eu.timepit.refined.types.string.NonEmptyString
import java.time.Instant

sealed trait Criteria extends Product with Serializable

object Criteria {
  final case class In(names: List[NonEmptyString])              extends Criteria
  final case class TaskCreatedDate(op: Operator, date: Instant) extends Criteria
  final case class State(status: Status)                        extends Criteria
}
