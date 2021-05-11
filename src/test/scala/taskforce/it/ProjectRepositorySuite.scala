package taskforce.it

import cats.effect.IO
import cats.implicits._
import org.scalacheck.effect.PropF
import taskforce.arbitraries._
import taskforce.model.NewProject

import taskforce.repos.LiveProjectRepository
import taskforce.repos.ProjectRepository

class ProjectRepositorySuite extends BasicRepositorySuite {

  var projectRepo: IO[ProjectRepository[IO]] = null

  override def beforeAll(): Unit = {
    super.beforeAll()
    projectRepo = LiveProjectRepository.make[IO](xa)

  }

  test("Project Creation test #1") {
    PropF.forAllF { (p: NewProject) =>
      for {
        repo      <- projectRepo
        allBefore <- repo.getAllProject
        project   <- repo.createProject(p, userID)
        allAfter  <- repo.getAllProject
      } yield assertEquals((allBefore.size, allAfter.filter(_.name == p.name).size, allAfter.size), (2, 1, 3))
    }
  }

  test("created project can be retrieved") {
    PropF.forAllF { (p: NewProject) =>
      for {
        repo      <- projectRepo
        created   <- repo.createProject(p, userID)
        retrieved <- repo.getProject(created.id)
      } yield assertEquals(created.some, retrieved)
    }
  }

  test("Rename for existing name will fail") {
    PropF.forAllF { (p1: NewProject, p2: NewProject) =>
      for {
        repo     <- projectRepo
        created1 <- repo.createProject(p1, userID)
        created2 <- repo.createProject(p2, userID)
        result   <- repo.renameProject(created1.id, p2).attempt
      } yield assert(result.isLeft)
    }
  }

  test("Project after deletion has 'deleted' field set") {
    PropF.forAllF { (p1: NewProject) =>
      for {
        repo      <- projectRepo
        created   <- repo.createProject(p1, userID)
        _         <- repo.deleteProject(created.id)
        retrieved <- repo.getProject(created.id)
      } yield assert(clue(retrieved.flatMap(_.deleted).isDefined))
    }

  }

}
