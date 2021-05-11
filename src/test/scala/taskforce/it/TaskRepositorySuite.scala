package taskforce.it

import cats.effect.IO
import cats.implicits._
import org.scalacheck.effect.PropF
import taskforce.arbitraries._
import taskforce.repos.{TaskRepository, LiveTaskRepository}
import taskforce.model.{ProjectId, Task}

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
        allBefore <- repo.getAllTasks(ProjectId(1)).compile.toList
        task      <- repo.createTask(t.copy(projectId = ProjectId(1), author = userID))
        allAfter  <- repo.getAllTasks(ProjectId(1)).compile.toList
      } yield assertEquals((allAfter.find(_ == task), allAfter.size), (Some(task), allBefore.size + 1))
    }
  }

  test("created project can be retrieved") {
    PropF.forAllF { (t: Task) =>
      for {
        repo      <- taskRepo
        created   <- repo.createTask(t.copy(projectId = ProjectId(1), author = userID))
        retrieved <- repo.getTask(ProjectId(1), t.id)
      } yield assertEquals(created.some, retrieved)
    }
  }

}
