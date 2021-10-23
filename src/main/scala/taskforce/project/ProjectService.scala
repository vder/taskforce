package taskforce.project

import cats.{Applicative, MonadError}
import cats.effect.Sync
import cats.implicits._
import taskforce.authentication.UserId
import taskforce.common.errors._

final class ProjectService[F[_]: Sync: Applicative: MonadError[*[_], Throwable]](
    projectRepo: ProjectRepository[F]
) {

  def totalTime(projectId: ProjectId) = find(projectId) *> projectRepo.totalTime(projectId)

  def list: F[List[Project]] = projectRepo.list

  def delete(projectId: ProjectId, actionBy: UserId) =
    for {
      projectOption <- projectRepo.find(projectId)
      project <-
        MonadError[F, Throwable]
          .fromOption(projectOption, NotFound(projectId.value.toString()))
          .ensure(NotAuthor(actionBy))(_.author == actionBy)
      rowsCount <- projectRepo.delete(projectId)
    } yield rowsCount

  def find(projectId: ProjectId) =
    projectRepo
      .find(projectId)
      .ensure(NotFound(projectId.value.toString()))(_.isDefined)

  def create(newProject: NewProject, userId: UserId) =
    projectRepo.create(newProject, userId)

  def update(projectId: ProjectId, newProject: NewProject, userId: UserId) =
    for {
      previousProject <-
        projectRepo
          .find(projectId)
          .ensure(NotFound(projectId.value.toString()))(_.isDefined)
          .ensure(NotAuthor(userId))(_.filter(_.author == userId).isDefined)
      project <-
        projectRepo
          .update(projectId, newProject)
    } yield project
}

object ProjectService {
  def make[F[_]: Sync](projectRepo: ProjectRepository[F]) =
    Sync[F].delay(
      new ProjectService[F](projectRepo)
    )
}
