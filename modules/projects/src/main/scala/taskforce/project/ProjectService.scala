package taskforce.project

import cats.implicits._
import taskforce.authentication.UserId
import taskforce.common.AppError
import cats.MonadThrow

final class ProjectService[F[_]: MonadThrow] private (
    projectRepo: ProjectRepository[F]
) {

  def totalTime(projectId: ProjectId): F[TotalTime] = projectRepo.totalTime(projectId)

  def list: F[List[Project]] = projectRepo.list

  def delete(projectId: ProjectId, actionBy: UserId): F[Int] =
    for {
      _         <- validateProjectExistsAndUserIsAuthor(projectId, actionBy)
      rowsCount <- projectRepo.delete(projectId)
    } yield rowsCount

  def find(projectId: ProjectId): F[Option[Project]] =
    projectRepo
      .find(projectId)

  def create(newProject: ProjectName, actionBy: UserId): F[Either[AppError.DuplicateProjectName, Project]] =
    projectRepo.create(newProject, actionBy)

  def update(
      projectId: ProjectId,
      newProject: ProjectName,
      actionBy: UserId
  ): F[Either[AppError.DuplicateProjectName, Project]] =
    for {
      _ <- validateProjectExistsAndUserIsAuthor(projectId, actionBy)
      project <-
        projectRepo
          .update(projectId, newProject)
    } yield project

  private def validateProjectExistsAndUserIsAuthor(projectId: ProjectId, userId: UserId) =
    projectRepo
      .find(projectId)
      .flatMap(_.toRight(AppError.NotFound(projectId.value.toString)).liftTo[F])
      .ensure(AppError.NotAuthor(userId.value))(_.author == userId)
}

object ProjectService {
  def make[F[_]: MonadThrow](projectRepo: ProjectRepository[F]): ProjectService[F] = new ProjectService[F](projectRepo)

}
