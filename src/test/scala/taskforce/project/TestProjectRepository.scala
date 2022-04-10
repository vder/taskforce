package taskforce.project

import cats.effect.IO
import cats.implicits._
import java.time.Duration
import java.util.UUID
import taskforce.authentication.UserId
import taskforce.common.CreationDate
import java.time.LocalDateTime

case class TestProjectRepository(projects: List[Project], currentTime: LocalDateTime) extends ProjectRepository[IO] {

  override def totalTime(projectId: ProjectId): IO[TotalTime] = TotalTime(Duration.ZERO).pure[IO]

  override def create(newProject: ProjectName, userId: UserId): IO[Either[DuplicateProjectNameError, Project]] =
    Project(
      author = userId,
      deleted = None,
      name = newProject,
      id = ProjectId(1),
      created = CreationDate(currentTime)
    ).asRight[DuplicateProjectNameError]
      .pure[IO]

  override def delete(id: ProjectId): IO[Int] = 1.pure[IO]

  override def update(
      id: ProjectId,
      newProject: ProjectName
  ): IO[Either[DuplicateProjectNameError, Project]] =
    Project(
      author = UserId(UUID.randomUUID()),
      deleted = None,
      name = newProject,
      id = id,
      created = CreationDate(currentTime)
    ).asRight[DuplicateProjectNameError]
      .pure[IO]

  override def find(id: ProjectId): IO[Option[Project]] = projects.find(_.id == id).pure[IO]

  override def list: IO[List[Project]] = projects.toList.pure[IO]

}
