package taskforce

import taskforce.model.domain.Task
import taskforce.model.errors.WrongPeriodError
import fs2.Stream
import java.time.Duration
import cats.implicits._
import cats.effect.Sync

object Validations {
  def taskPeriodIsValid[F[_]: Sync](
      newTask: Task,
      userTasks: Stream[F, Task]
  ): F[Boolean] = {
    val taskEnd = newTask.created.plus(newTask.duration.value)
    val taskStart = newTask.created
    for {
      isValid <-
        userTasks
          .collect {
            case t if t.deleted.isEmpty =>
              (t.created, t.created.plus(t.duration.value))
          }
          .forall {
            case (start, end) =>
              (start.isAfter(taskEnd) || taskStart.isAfter(end))
          }
          .compile
          .toList
          .ensure(WrongPeriodError)(_.head)
    } yield isValid.head

  }

}
