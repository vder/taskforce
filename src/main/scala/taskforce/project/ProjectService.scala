package taskforce.project

import cats.effect.Sync
import cats.implicits._
import taskforce.authentication.UserId
import taskforce.common.errors._
import cats.MonadThrow

final class ProjectService[F[_]: Sync](
    projectRepo: ProjectRepository[F]
) {

  def totalTime(projectId: ProjectId) = find(projectId) *> projectRepo.totalTime(projectId)

  def list: F[List[Project]] = projectRepo.list

  def delete(projectId: ProjectId, actionBy: UserId) =
    for {
      projectOption <- projectRepo.find(projectId)
      _ <-
        MonadThrow[F]
          .fromOption(projectOption, NotFound(projectId.value.toString()))
          .ensure(NotAuthor(actionBy.value))(_.author == actionBy)
      rowsCount <- projectRepo.delete(projectId)
    } yield rowsCount

  def find(projectId: ProjectId) =
    projectRepo
      .find(projectId)
      .ensure(NotFound(projectId.value.toString()))(_.isDefined)

  def create(newProject: ProjectName, userId: UserId) =
    projectRepo.create(newProject, userId)

  def update(projectId: ProjectId, newProject: ProjectName, userId: UserId) =
    for {
      _ <-
        projectRepo
          .find(projectId)
          .ensure(NotFound(projectId.value.toString()))(_.isDefined)
          .ensure(NotAuthor(userId.value))(_.filter(_.author == userId).isDefined)
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
