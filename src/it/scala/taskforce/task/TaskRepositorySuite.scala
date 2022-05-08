package taskforce.task

import cats.effect.IO
import org.scalacheck.effect.PropF
import taskforce.task.arbitraries._
import taskforce.task.ProjectId
import taskforce.BasicRepositorySuite

class TaskRepositorySuite extends BasicRepositorySuite {

  var taskRepo: IO[TaskRepository[IO]] = null

  override def beforeAll(): Unit = {
    super.beforeAll()
    taskRepo = LiveTaskRepository.make[IO](xa)

  }

  test("task creation") {
    PropF.forAllF { (t: Task) =>
      for {
        repo      <- taskRepo
        allBefore <- repo.list(ProjectId(1)).compile.toList
        task = t.copy(projectId = ProjectId(1), author = userID)
        taskResult <- repo.create(task)
        allAfter   <- repo.list(ProjectId(1)).compile.toList
      } yield assertEquals(
        (allAfter.find(_ == task).toRight(DuplicateTaskNameError(task)), allAfter.size),
        (taskResult, allBefore.size + 1)
      )
    }
  }

  test("created project can be retrieved") {
    PropF.forAllF { (t: Task) =>
      for {
        repo      <- taskRepo
        created   <- repo.create(t.copy(projectId = ProjectId(1), author = userID))
        retrieved <- repo.find(ProjectId(1), t.id)
      } yield assertEquals(created, retrieved.toRight(DuplicateTaskNameError(t)))
    }
  }

}
