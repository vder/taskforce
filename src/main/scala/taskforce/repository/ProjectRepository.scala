package taskforce.repository

import cats.Monad
import cats.effect.Bracket
import cats.effect.Sync
import cats.syntax.all._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.meta.Meta
import doobie.util.transactor.Transactor
import eu.timepit.refined.types.string.NonEmptyString
import java.time.LocalDateTime
import java.util.UUID
import taskforce.model.domain._
import taskforce.model.errors._

trait ProjectRepository[F[_]] {
  def createProject(newProject: NewProject, userId: UserId): F[Project]
  def deleteProject(id: ProjectId): F[Int]
  def renameProject(
      id: ProjectId,
      newProject: NewProject,
      userId: UserId
  ): F[Project]
  def getProject(id: ProjectId): F[Option[Project]]
  def getAllProject: F[List[Project]]
}

final class LiveProjectRepository[F[_]: Monad: Bracket[*[_], Throwable]](
    xa: Transactor[F]
) extends ProjectRepository[F] {

  override def getProject(id: ProjectId): F[Option[Project]] =
    sql"""select id,name,author,created,deleted from projects where id=${id.value}"""
      .query[Project]
      .option
      .transact(xa)

  override def getAllProject: F[List[Project]] =
    sql"""select id,name,author,created,deleted from projects"""
      .query[Project]
      .stream
      .compile
      .toList
      .transact(xa)

  override def createProject(
      newProject: NewProject,
      userId: UserId
  ): F[Project] =
    sql"""insert into projects(name,author,created) 
          values(${newProject.name.value},${userId.id},CURRENT_TIMESTAMP) 
          returning id,created,name""".update
      .withUniqueGeneratedKeys[
        (Long, LocalDateTime, NonEmptyString)
      ](
        "id",
        "created",
        "name"
      )
      .map {
        case (id, created, name) =>
          Project(ProjectId(id), name, userId, created, None)
      }
      .transact(xa)

  override def deleteProject(id: ProjectId): F[Int] =
    sql""" update projects set deleted = CURRENT_TIMESTAMP where id =$id""".update.run
      .transact(xa)

  override def renameProject(
      id: ProjectId,
      newProject: NewProject,
      userId: UserId
  ): F[Project] = {
    val result = for {
      _ <-
        sql"""select author from projects where id=${id.value}"""
          .query[UUID]
          .unique
          .ensure(NotAuthorError(userId))(_ == userId.id)
      dates <-
        sql"""update projects set name= ${newProject.name} where id=${id.value}
              returning created,deleted""".update
          .withUniqueGeneratedKeys[(LocalDateTime, Option[LocalDateTime])](
            "created",
            "deleted"
          )
      (created, deleted) = dates
    } yield Project(id, newProject.name, userId, created, deleted)

    result.transact(xa)

  }
}

object LiveProjectRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveProjectRepository[F](xa) }
}
