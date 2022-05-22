package taskforce.task

import scala.util.control.NoStackTrace

sealed trait TaskError extends NoStackTrace

final case class DuplicateTaskNameError(task: Task) extends TaskError
final case object WrongPeriodError                  extends TaskError
