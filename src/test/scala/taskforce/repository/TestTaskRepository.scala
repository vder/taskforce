package taskforce.repository

import taskforce.model._
import cats.implicits._
import cats.effect.IO
import fs2.Stream

final case class TestTaskRepository(tasks: List[Task]) extends TaskRepository[IO] {
  def createTask(task: Task): IO[Task] = task.pure[IO]

  def deleteTask(id: TaskId): IO[Int] = 1.pure[IO]

  def getTask(projectId: ProjectId, taskId: TaskId): IO[Option[Task]] =
    tasks.find(t => t.id == taskId && t.projectId == projectId).pure[IO]

  def getAllTasks(projectId: ProjectId): Stream[IO, Task] = Stream.emits(tasks)

  def getAllUserTasks(author: UserId): fs2.Stream[IO, Task] = Stream.emits(tasks).filter(_.author == author)

  def updateTask(id: TaskId, task: Task): IO[Task] = task.pure[IO]
}
