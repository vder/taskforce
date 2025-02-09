package taskforce.task

import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import doobie.implicits._
import doobie.util.transactor.Transactor
import org.postgresql.util.PSQLException
import taskforce.authentication.UserId
import fs2.Stream
import taskforce.common.DeletionDate
import taskforce.common.AppError
import cats.effect.kernel.Clock

trait TaskRepository[F[_]] {
  def create(task: Task): F[Either[AppError.DuplicateTaskNameError, Task]]
  def delete(id: TaskId): F[Int]
  def find(projectId: ProjectId, taskId: TaskId): F[Option[Task]]
  def list(projectId: ProjectId): Stream[F, Task]
  def listByUser(author: UserId): Stream[F, Task]
  def update(id: TaskId, task: Task): F[Either[AppError.DuplicateTaskNameError, Task]]
}

object TaskRepository {

  def make[F[_]: MonadCancelThrow: Clock](xa: Transactor[F]): TaskRepository[F] =
    new TaskRepository[F] with instances.Doobie {

      import ctx._
      private def mapDatabaseErr(
          task: Task
      ): PartialFunction[Throwable, Either[AppError.DuplicateTaskNameError, Task]] = {
        case x: PSQLException
            if x.getMessage.contains(
              "unique constraint"
            ) =>
          AppError.DuplicateTaskNameError(task.id.value.toString()).asLeft[Task]
      }

      override def update(
          id: TaskId,
          task: Task
      ): F[Either[AppError.DuplicateTaskNameError, Task]] = {

        def update(deletionDate: DeletionDate) = for {
          _ <- run(
            taskQuery
              .filter(p => p.id == lift(id) && p.deleted.isEmpty)
              .update(_.deleted -> lift(deletionDate.some))
          )
          _ <- run(taskQuery.insertValue(lift(task)))
        } yield ()

        for {
          deletionDate <- Clock[F].realTimeInstant.map(DeletionDate(_))
          result <- update(deletionDate)
            .transact(xa)
            .as(task.asRight[AppError.DuplicateTaskNameError])
            .recover(mapDatabaseErr(task))
        } yield result

      }

      override def listByUser(author: UserId): Stream[F, Task] =
        stream(taskQuery.filter(_.author == lift(author)))
          .transact(xa)

      override def find(projectId: ProjectId, taskId: TaskId): F[Option[Task]] =
        run(
          taskQuery.filter(t => t.projectId == lift(projectId) && t.id == lift(taskId))
        )
          .transact(xa)
          .map(_.headOption)

      override def create(task: Task): F[Either[AppError.DuplicateTaskNameError, Task]] =
        run(taskQuery.insertValue(lift(task)))
          .transact(xa)
          .as(task.asRight[AppError.DuplicateTaskNameError])
          .recover(mapDatabaseErr(task))

      def delete(id: TaskId): F[Int] =
        for {
          deletionDate <- Clock[F].realTimeInstant.map(DeletionDate(_))
          result <- run(
            taskQuery
              .filter(p => p.id == lift(id) && p.deleted.isEmpty)
              .update(_.deleted -> lift(deletionDate.some))
          )
            .transact(xa)
            .map(_.toInt)
        } yield result

      override def list(projectId: ProjectId): Stream[F, Task] =
        stream(taskQuery.filter(_.projectId == lift(projectId)))
          .transact(xa)

    }

}
