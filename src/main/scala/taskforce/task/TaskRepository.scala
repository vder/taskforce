package taskforce.task

import cats.Monad
import cats.effect.Sync
import cats.effect.kernel.MonadCancel
import cats.syntax.all._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import fs2._
import org.postgresql.util.PSQLException
import taskforce.authentication.UserId
import taskforce.project.ProjectId

trait TaskRepository[F[_]] {
  def create(task: Task): F[Either[DuplicateTaskNameError, Task]]
  def delete(id: TaskId): F[Int]
  def find(projectId: ProjectId, taskId: TaskId): F[Option[Task]]
  def list(projectId: ProjectId): Stream[F, Task]
  def listByUser(author: UserId): Stream[F, Task]
  def update(id: TaskId, task: Task): F[Either[DuplicateTaskNameError, Task]]
}

final class LiveTaskRepository[F[_]: Monad: MonadCancel[*[_], Throwable]](
    xa: Transactor[F]
) extends TaskRepository[F]
    with instances.Doobie {

  private def mapDatabaseErr(task: Task): PartialFunction[Throwable, Either[DuplicateTaskNameError, Task]] = {
    case x: PSQLException
        if x.getMessage.contains(
          "unique constraint"
        ) =>
      DuplicateTaskNameError(task).asLeft[Task]
  }

  override def update(id: TaskId, task: Task): F[Either[DuplicateTaskNameError, Task]] = {
    val update = for {
      _ <- sql.delete(id).update.run
      _ <- sql.create(task).update.run
    } yield ()
    update.transact(xa).as(task.asRight[DuplicateTaskNameError]).recover(mapDatabaseErr(task))

  }

  override def listByUser(author: UserId): Stream[F, Task] =
    sql
      .getAllByAuthor(author)
      .query[Task]
      .stream
      .transact(xa)

  override def find(projectId: ProjectId, taskId: TaskId): F[Option[Task]] =
    sql
      .getOne(projectId, taskId)
      .query[Task]
      .option
      .transact(xa)

  override def create(task: Task): F[Either[DuplicateTaskNameError, Task]] =
    sql
      .create(task)
      .update
      .run
      .transact(xa)
      .as(task.asRight[DuplicateTaskNameError])
      .recover(mapDatabaseErr(task))

  override def delete(id: TaskId): F[Int] =
    sql
      .delete(id)
      .update
      .run
      .transact(xa)

  override def list(projectId: ProjectId): Stream[F, Task] =
    sql
      .getAll(projectId)
      .query[Task]
      .stream
      .transact(xa)

  object sql {

    def getAllByAuthor(author: UserId) =
      sql"""select id,
            |      project_id,
            |      author,
            |      started,
            |      duration,
            |      volume,
            |      deleted,
            |      comment
            | from tasks where author = ${author.value}""".stripMargin

    def getOne(projectId: ProjectId, taskId: TaskId) =
      sql"""select id,
           |       project_id,
           |       author,
           |       started,
           |       duration,
           |       volume,
           |       deleted,
           |       comment
           |  from tasks 
           | where id = ${taskId} 
           |   and project_id = ${projectId}""".stripMargin

    def getAll(projectId: ProjectId) =
      sql"""select id,project_id,author,started,duration,volume,deleted,comment from tasks where project_id = ${projectId}"""

    def delete(id: TaskId) =
      sql"""update tasks set deleted = CURRENT_TIMESTAMP where id =${id} and deleted is null"""

    def create(task: Task) =
      sql"""insert into tasks(id,project_id,author,started,duration,volume,comment)
            | values(${task.id.value},
            |        ${task.projectId.value},
            |        ${task.author.value},
            |        ${task.created},
            |        ${task.duration},
            |        ${task.volume},
            |        ${task.comment})""".stripMargin
  }

}

object LiveTaskRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveTaskRepository[F](xa) }
}
