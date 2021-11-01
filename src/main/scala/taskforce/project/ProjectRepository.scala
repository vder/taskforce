package taskforce.project

import cats.effect.Sync
import cats.effect.kernel.MonadCancel
import cats.syntax.all._
import doobie.implicits._
import doobie.util.transactor.Transactor
import java.time.LocalDateTime
import org.postgresql.util.PSQLException
import taskforce.authentication.UserId
import doobie.quill.DoobieContext
import io.getquill.Literal
import taskforce.task.Task
import eu.timepit.refined.types.string
import eu.timepit.refined.api.Refined
import taskforce.task.TaskDuration
import taskforce.task.instances.{Doobie => doobieTaskInstances}
import java.time.Duration

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
    with instances.Doobie
    with doobieTaskInstances {

  val ctx = new DoobieContext.Postgres(Literal)
  import ctx._

  val projectQuery = quote {
    querySchema[Project]("projects")
  }

  val taskQuery = quote {
    querySchema[Task]("tasks", _.projectId -> "project_id")
  }

  val newProjectId = ProjectId(0L)

  implicit val decodeNonEmptyString = MappedEncoding[String, string.NonEmptyString](Refined.unsafeApply(_))
  implicit val encodeNonEmptyString = MappedEncoding[string.NonEmptyString, String](_.value)
  implicit val taskDurationNumeric  = fakeNumeric[TaskDuration]

  def mapDatabaseErr(newProject: NewProject): PartialFunction[Throwable, Either[DuplicateProjectNameError, Project]] = {
    case x: PSQLException
        if x.getMessage.contains(
          "unique constraint"
        ) =>
      DuplicateProjectNameError(newProject).asLeft[Project]
  }

  override def totalTime(projectId: ProjectId): F[TotalTime] =
    run(taskQuery.filter(t => t.projectId == lift(projectId) && t.deleted.isEmpty).map(_.duration).sum)
      .transact(xa)
      .map(t => TotalTime(t.getOrElse(TaskDuration(Duration.ZERO)).value))

  override def find(id: ProjectId): F[Option[Project]] =
    run(projectQuery.filter(_.id == lift(id))).transact(xa).map(_.headOption)

  override def list: F[List[Project]] =
    run(projectQuery)
      .transact(xa)

  override def create(
      newProject: NewProject,
      author: UserId
  ): F[Either[DuplicateProjectNameError, Project]] = {
    val created = LocalDateTime.now()
    run(
      projectQuery
        .insert(lift(Project(newProjectId, newProject.name, author, created, None)))
        .returningGenerated(_.id)
    )
      .transact(xa)
      .map { case id =>
        Project(id, newProject.name, author, created, None)
      }
      .map(_.asRight[DuplicateProjectNameError])
      .recover(mapDatabaseErr(newProject))
  }

  override def delete(id: ProjectId): F[Int] = {
    val deleted = LocalDateTime.now().some
    val result = for {
      x <- run(projectQuery.filter(p => p.id == lift(id) && p.deleted.isEmpty).update(_.deleted -> lift(deleted)))
      y <- run(taskQuery.filter(t => t.projectId == lift(id) && t.deleted.isEmpty).update(_.deleted -> lift(deleted)))
    } yield (x + y)

    result.transact(xa).map(_.toInt)
  }

  override def update(
      id: ProjectId,
      newProject: NewProject
  ): F[Either[DuplicateProjectNameError, Project]] = {
    run(
      projectQuery
        .filter(_.id == lift(id))
        .update(_.name -> lift(newProject.name))
        .returning(p => p)
    )
      .transact(xa)
      .map(_.asRight[DuplicateProjectNameError])
      .recover(mapDatabaseErr(newProject))

  }

}

object LiveProjectRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveProjectRepository[F](xa) }
}
