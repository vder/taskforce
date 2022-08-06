package taskforce.project

import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.getquill.{NamingStrategy, PluralizedTableNames, SnakeCase}
import org.polyvariant.doobiequill._
import org.postgresql.util.PSQLException
import taskforce.authentication.UserId
import taskforce.common.NewTypeQuillInstances
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.Instant
import taskforce.common.{CreationDate, DeletionDate}
import taskforce.common.AppError

trait ProjectRepository[F[_]] {
  def create(newProject: ProjectName, userId: UserId): F[Either[AppError.DuplicateProjectName, Project]]
  def delete(id: ProjectId): F[Int]
  def update(
      id: ProjectId,
      newProject: ProjectName
  ): F[Either[AppError.DuplicateProjectName, Project]]
  def find(id: ProjectId): F[Option[Project]]
  def list: F[List[Project]]
  def totalTime(projectId: ProjectId): F[TotalTime]
}

object ProjectRepository {
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]) =
    new ProjectRepository[F] with instances.Doobie with NewTypeQuillInstances {

      private val ctx = new DoobieContext.Postgres(NamingStrategy(PluralizedTableNames, SnakeCase))
      import ctx._

      private val projectQuery = quote {
        query[Project]
      }
      private val taskQuery = quote {
        querySchema[TaskTime]("tasks", _.time -> "duration")
      }
      private val newProjectId = ProjectId(0L)

      override def totalTime(projectId: ProjectId): F[TotalTime] =
        run(taskQuery.filter(t => t.projectId == lift(projectId) && t.deleted.isEmpty).map(_.time).sum)
          .transact(xa)
          .map(t => t.getOrElse(TotalTime(Duration.ZERO)))

      override def find(id: ProjectId): F[Option[Project]] =
        run(query[Project].filter(_.id == lift(id))).transact(xa).map(_.headOption)

      override def list: F[List[Project]] =
        run(projectQuery)
          .transact(xa)

      override def create(
          newProject: ProjectName,
          author: UserId
      ): F[Either[AppError.DuplicateProjectName, Project]] = {
        val created = CreationDate(Instant.now().truncatedTo(ChronoUnit.SECONDS))
        run(
          projectQuery
            .insertValue(lift(Project(newProjectId, newProject, author, created, None)))
            .returningGenerated(_.id)
        )
          .transact(xa)
          .map { id =>
            Project(id, newProject, author, created, None)
          }
          .map(_.asRight[AppError.DuplicateProjectName])
          .recover(mapDatabaseErr(newProject))
      }

      override def delete(id: ProjectId): F[Int] = {
        val deleted = DeletionDate(Instant.now()).some
        val result = for {
          x <- run(projectQuery.filter(p => p.id == lift(id) && p.deleted.isEmpty).update(_.deleted -> lift(deleted)))
          y <- run(
            taskQuery.filter(t => t.projectId == lift(id) && t.deleted.isEmpty).update(_.deleted -> lift(deleted))
          )
        } yield x + y

        result.transact(xa).map(_.toInt)
      }

      override def update(
          id: ProjectId,
          newProject: ProjectName
      ): F[Either[AppError.DuplicateProjectName, Project]] = {
        run(
          projectQuery
            .filter(_.id == lift(id))
            .update(_.name -> lift(newProject))
            .returning(p => p)
        )
          .transact(xa)
          .map(_.asRight[AppError.DuplicateProjectName])
          .recover(mapDatabaseErr(newProject))

      }

      private def mapDatabaseErr(
          newProject: ProjectName
      ): PartialFunction[Throwable, Either[AppError.DuplicateProjectName, Project]] = {
        case x: PSQLException
            if x.getMessage.contains(
              "unique constraint"
            ) =>
          AppError.DuplicateProjectName(s"$newProject").asLeft[Project]
      }

      case class TaskTime(projectId: ProjectId, time: TotalTime, deleted: Option[DeletionDate])

    }
}
