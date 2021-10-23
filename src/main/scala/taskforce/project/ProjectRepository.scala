package taskforce.project

import cats.effect.Sync
import cats.effect.kernel.MonadCancel
import cats.syntax.all._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import eu.timepit.refined.types.string.NonEmptyString
import java.time.LocalDateTime
import org.postgresql.util.PSQLException
import taskforce.authentication.UserId

trait ProjectRepository[F[_]] {
  def create(newProject: NewProject, userId: UserId): F[Either[DuplicateProjectNameError, Project]]
  def delete(id: ProjectId): F[Int]
  def update(
      id: ProjectId,
      newProject: NewProject
  ): F[Either[DuplicateProjectNameError, Project]]
  def find(id: ProjectId): F[Option[Project]]
  def list: F[List[Project]]
  def totalTime(projectId: ProjectId): F[TotalTime]
}

final class LiveProjectRepository[F[_]: MonadCancel[*[_], Throwable]](
    xa: Transactor[F]
) extends ProjectRepository[F]
    with instances.Doobie {

  def mapDatabaseErr(newProject: NewProject): PartialFunction[Throwable, Either[DuplicateProjectNameError, Project]] = {
    case x: PSQLException
        if x.getMessage.contains(
          "unique constraint"
        ) =>
      DuplicateProjectNameError(newProject).asLeft[Project]
  }

  override def totalTime(projectId: ProjectId): F[TotalTime] =
    sql.getTotalTime(projectId).query[TotalTime].unique.transact(xa)

  override def find(id: ProjectId): F[Option[Project]] =
    sql
      .getOne(id)
      .query[Project]
      .option
      .transact(xa)

  override def list: F[List[Project]] =
    sql.getAll
      .query[Project]
      .stream
      .compile
      .toList
      .transact(xa)

  override def create(
      newProject: NewProject,
      author: UserId
  ): F[Either[DuplicateProjectNameError, Project]] =
    sql
      .create(newProject.name, author)
      .update
      .withUniqueGeneratedKeys[
        (Long, LocalDateTime, NonEmptyString)
      ](
        "id",
        "created",
        "name"
      )
      .map { case (id, created, name) =>
        Project(ProjectId(id), name, author, created, None)
      }
      .transact(xa)
      .map(_.asRight[DuplicateProjectNameError])
      .recover(mapDatabaseErr(newProject))

  override def delete(id: ProjectId): F[Int] = {
    val result = for {
      x <- sql.deleteProject(id).stripMargin.update.run
      y <- sql.deleteTask(id).update.run
    } yield (x + y)

    result.transact(xa)
  }

  override def update(
      id: ProjectId,
      newProject: NewProject
  ): F[Either[DuplicateProjectNameError, Project]] = {
    val result = for {
      (created, deleted, userId) <-
        sql
          .update(id, newProject.name)
          .update
          .withUniqueGeneratedKeys[
            (LocalDateTime, Option[LocalDateTime], UserId)
          ](
            "created",
            "deleted",
            "author"
          )
      //    totalTime <- sql.totalTime(id).query[Long].unique
    } yield Project(id, newProject.name, userId, created, deleted).asRight[DuplicateProjectNameError]

    result.transact(xa).recover(mapDatabaseErr(newProject))

  }

  object sql {

    def getTotalTime(projectId: ProjectId) =
      sql"""select coalesce(sum(duration),0) 
           |  from tasks 
           | where project_id = ${projectId}
           |   and deleted is null""".stripMargin

    def create(projectName: NonEmptyString, author: UserId) =
      sql"""insert into projects(name,author,created) 
       |    values (${projectName.value},
       |            ${author.value},
       |            CURRENT_TIMESTAMP) 
       | returning id,created,name""".stripMargin

    def update(projectId: ProjectId, projectName: NonEmptyString) =
      sql"""update projects 
       |       set name= ${projectName.value} 
       |     where id=${projectId.value}
       | returning created,deleted,author""".stripMargin

    def totalTime(projectId: ProjectId) =
      sql"""select sum(duration) 
           |  from tasks 
           | where project_id =${projectId.value}
           |   and deleted is null""".stripMargin

    def deleteProject(projectId: ProjectId) =
      sql"""update projects 
           |   set deleted = CURRENT_TIMESTAMP
           | where id =${projectId} and deleted is null""".stripMargin

    def deleteTask(projectId: ProjectId) =
      sql"""update tasks 
          |   set deleted = CURRENT_TIMESTAMP
          | where project_id =$projectId and deleted is null""".stripMargin

    def getOne(projectId: ProjectId) =
      sql"""select id,
           |       name,
           |       author,
           |       created,
           |       deleted
           |  from projects 
           | where id=${projectId}""".stripMargin
    val getAll =
      sql"""select id,
              |    name,
              |    author,
              |    created,
              |    deleted
              | from projects p""".stripMargin
  }
}

object LiveProjectRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveProjectRepository[F](xa) }
}
