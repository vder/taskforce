package taskforce.task

import cats.effect.IO
import cats.implicits._
import fs2.Stream
import taskforce.authentication.UserId
import taskforce.project.ProjectId

final case class TestTaskRepository(tasks: List[Task]) extends TaskRepository[IO] {
  override def create(task: Task) = task.asRight[DuplicateTaskNameError].pure[IO]

  override def delete(id: TaskId): IO[Int] = 1.pure[IO]

  override def find(projectId: ProjectId, taskId: TaskId): IO[Option[Task]] =
    tasks.find(t => t.id == taskId && t.projectId == projectId).pure[IO]

  override def list(projectId: ProjectId): Stream[IO, Task] = Stream.emits(tasks)

  override def listByUser(author: UserId): fs2.Stream[IO, Task] = Stream.emits(tasks).filter(_.author == author)

  override def update(id: TaskId, task: Task) = task.asRight[DuplicateTaskNameError].pure[IO]
}
