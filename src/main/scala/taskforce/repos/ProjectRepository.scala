package taskforce.repos

import cats.MonadError
import cats.effect.Bracket
import cats.effect.Sync
import cats.syntax.all._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import eu.timepit.refined.types.string.NonEmptyString
import java.time.LocalDateTime
import org.postgresql.util.PSQLException
import taskforce.model._
import taskforce.model.errors._
trait ProjectRepository[F[_]] {
  def createProject(newProject: NewProject, userId: UserId): F[Project]
  def deleteProject(id: ProjectId): F[Int]
  def renameProject(
      id: ProjectId,
      newProject: NewProject
  ): F[Project]
  def getProject(id: ProjectId): F[Option[Project]]
  def getAllProject: F[List[Project]]
}

final class LiveProjectRepository[F[_]: Bracket[*[_], Throwable]: MonadError[*[_], Throwable]](
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
      author: UserId
  ): F[Project] =
    sql"""insert into projects(name,author,created) 
          |    values (${newProject.name.value},
          |            ${author.value},
          |            CURRENT_TIMESTAMP) 
          | returning id,created,name""".stripMargin.update
      .withUniqueGeneratedKeys[
        (Long, LocalDateTime, NonEmptyString)
      ](
        "id",
        "created",
        "name"
      )
      .map {
        case (id, created, name) =>
          Project(ProjectId(id), name, author, created, None)
      }
      .transact(xa)
      .adaptError {
        case x: PSQLException
            if x.getMessage.contains(
              "unique constraint"
            ) =>
          DuplicateNameError(newProject.name.value)
      }

  override def deleteProject(id: ProjectId): F[Int] = {
    val result = for {
      x <- sql"""update projects 
         |   set deleted = CURRENT_TIMESTAMP
         | where id =$id""".stripMargin.update.run
      y <- sql"""update tasks 
           |   set deleted = CURRENT_TIMESTAMP
           | where project_id =$id""".stripMargin.update.run
    } yield (x + y)

    result.transact(xa)
  }

  override def renameProject(
      id: ProjectId,
      newProject: NewProject
  ): F[Project] = {
    val result = for {
      (created, deleted, userId) <-
        sql"""update projects 
         |       set name= ${newProject.name} 
         |     where id=${id.value}
         | returning created,deleted,author""".stripMargin.update
          .withUniqueGeneratedKeys[
            (LocalDateTime, Option[LocalDateTime], UserId)
          ](
            "created",
            "deleted",
            "author"
          )
    } yield Project(id, newProject.name, userId, created, deleted)

    result.transact(xa).adaptError {
      case x: PSQLException
          if x.getMessage.contains(
            "unique constraint"
          ) =>
        DuplicateNameError(newProject.name.value)
    }

  }
}

object LiveProjectRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveProjectRepository[F](xa) }
}
