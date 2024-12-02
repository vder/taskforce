package taskforce.task

import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import doobie.implicits._
import org.polyvariant.doobiequill.DoobieContext
import doobie.util.transactor.Transactor
import org.postgresql.util.PSQLException
import taskforce.authentication.UserId
import fs2.Stream
import eu.timepit.refined.numeric
import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string
import java.time.Instant
import io.getquill.NamingStrategy
import io.getquill.PluralizedTableNames
import io.getquill.SnakeCase
import taskforce.common.DeletionDate

trait TaskRepository[F[_]] {
  def create(task: Task): F[Either[DuplicateTaskNameError, Task]]
  def delete(id: TaskId): F[Int]
  def find(projectId: ProjectId, taskId: TaskId): F[Option[Task]]
  def list(projectId: ProjectId): Stream[F, Task]
  def listByUser(author: UserId): Stream[F, Task]
  def update(id: TaskId, task: Task): F[Either[DuplicateTaskNameError, Task]]
}



object TaskRepository {
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]) =
  new  TaskRepository[F]
    with instances.Doobie {

  val ctx =
    new DoobieContext.Postgres(NamingStrategy(PluralizedTableNames, SnakeCase))
  import ctx._

  val taskQuery = quote {
    querySchema[Task]("tasks", _.created -> "started")
  }

  implicit val decodePositiveInt: MappedEncoding[Int,Refined[Int,numeric.Positive]] =
    MappedEncoding[Int, Int Refined numeric.Positive](Refined.unsafeApply(_))
  implicit val encodePositiveInt: MappedEncoding[Refined[Int,numeric.Positive],Int] =
    MappedEncoding[Int Refined numeric.Positive, Int](_.value)

  implicit val decodeNonEmptyString: MappedEncoding[String,string.NonEmptyString] =
    MappedEncoding[String, string.NonEmptyString](Refined.unsafeApply(_))
  implicit val encodeNonEmptyString: MappedEncoding[string.NonEmptyString,String] =
    MappedEncoding[string.NonEmptyString, String](_.value)

  private def mapDatabaseErr(
      task: Task
  ): PartialFunction[Throwable, Either[DuplicateTaskNameError, Task]] = {
    case x: PSQLException
        if x.getMessage.contains(
          "unique constraint"
        ) =>
      DuplicateTaskNameError(task).asLeft[Task]
  }

  override def update(
      id: TaskId,
      task: Task
  ): F[Either[DuplicateTaskNameError, Task]] = {
    val update = for {
      _ <- run(
        taskQuery
          .filter(p => p.id == lift(id) && p.deleted.isEmpty)
          .update(_.deleted -> lift(DeletionDate(Instant.now()).some))
      )
      _ <- run(taskQuery.insert(lift(task)))
    } yield ()
    update
      .transact(xa)
      .as(task.asRight[DuplicateTaskNameError])
      .recover(mapDatabaseErr(task))

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

  override def create(task: Task): F[Either[DuplicateTaskNameError, Task]] =
    run(taskQuery.insert(lift(task)))
      .transact(xa)
      .as(task.asRight[DuplicateTaskNameError])
      .recover(mapDatabaseErr(task))

  override def delete(id: TaskId): F[Int] =
    run(
      taskQuery
        .filter(p => p.id == lift(id) && p.deleted.isEmpty)
        .update(_.deleted -> lift(DeletionDate(Instant.now()).some))
    )
      .transact(xa)
      .map(_.toInt)

  override def list(projectId: ProjectId): Stream[F, Task] =
    stream(taskQuery.filter(_.projectId == lift(projectId)))
      .transact(xa)

}
}
