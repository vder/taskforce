package taskforce.repository

import cats.Monad
import cats.effect.Bracket
import cats.effect.Sync
import cats.syntax.all._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import fs2._
import taskforce.model._

trait TaskRepository[F[_]] {
  def createTask(task: Task): F[Task]
  def deleteTask(id: TaskId): F[Int]
  def getTask(projectId: ProjectId, taskId: TaskId): F[Option[Task]]
  def getAllTasks(projectId: Int): Stream[F, Task]
  def getAllUserTasks(userId: UserId): Stream[F, Task]
  def updateTask(id: TaskId, task: Task): F[Task]
}

final class LiveTaskRepository[F[_]: Monad: Bracket[*[_], Throwable]](
    xa: Transactor[F]
) extends TaskRepository[F] {

  override def updateTask(id: TaskId, task: Task): F[Task] = {
    val update = for {
      _ <-
        sql""" update tasks set deleted = CURRENT_TIMESTAMP where id =${id.value}""".update.run
      _ <-
        sql"""insert into tasks(id,project_id,author,started,duration,volume,comment)
          values(${task.id.value},${task.projectId.value},${task.owner.id},${task.created},${task.duration},${task.volume},${task.comment})
          """.update.run
    } yield ()
    update.transact(xa).as(task)

  }

  override def getAllUserTasks(userId: UserId): Stream[F, Task] =
    sql""" select id,project_id,author,started,duration,volume,deleted,comment from tasks where author = ${userId.id}"""
      .query[Task]
      .stream
      .transact(xa)

  override def getTask(projectId: ProjectId, taskId: TaskId): F[Option[Task]] =
    sql""" select id,project_id,author,started,duration,volume,deleted,comment from tasks where id = ${taskId.value} and project_id = ${projectId.value}"""
      .query[Task]
      .option
      .transact(xa)

  override def createTask(task: Task): F[Task] =
    sql"""insert into tasks(id,project_id,author,started,duration,volume,comment)
          values(${task.id.value},${task.projectId.value},${task.owner.id},${task.created},${task.duration},${task.volume},${task.comment})
          """.update.run
      .transact(xa)
      .as(task)

  override def deleteTask(id: TaskId): F[Int] =
    sql""" update tasks set deleted = CURRENT_TIMESTAMP where id =${id.value}""".update.run
      .transact(xa)

  override def getAllTasks(projectId: Int): Stream[F, Task] =
    sql""" select id,project_id,author,started,duration,volume,deleted,comment from tasks where project_id = ${projectId}"""
      .query[Task]
      .stream
      .transact(xa)

}

object LiveTaskRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveTaskRepository[F](xa) }
}
