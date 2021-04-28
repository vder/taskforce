package taskforce

import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import taskforce.model.Task
import taskforce.model.errors.WrongPeriodError

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
