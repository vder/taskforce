package taskforce.repos

import cats.implicits._
import taskforce.model.Project
import taskforce.model.NewProject
import taskforce.model.UserId
import taskforce.model.ProjectId
import java.time.LocalDateTime
import java.util.UUID
import cats.effect.IO

case class TestProjectRepository(projects: List[Project], currentTime: LocalDateTime) extends ProjectRepository[IO] {

  override def createProject(newProject: NewProject, userId: UserId): IO[Project] =
    Project(
      author = userId,
      deleted = None,
      name = newProject.name,
      id = ProjectId(1),
      created = currentTime
    )
      .pure[IO]

  override def deleteProject(id: ProjectId): IO[Int] = 1.pure[IO]

  override def renameProject(id: ProjectId, newProject: NewProject): IO[Project] =
    Project(
      author = UserId(UUID.randomUUID()),
      deleted = None,
      name = newProject.name,
      id = id,
      created = currentTime
    )
      .pure[IO]

  override def getProject(id: ProjectId): IO[Option[Project]] = projects.find(_.id == id).pure[IO]

  override def getAllProject: IO[List[Project]] = projects.toList.pure[IO]

}
