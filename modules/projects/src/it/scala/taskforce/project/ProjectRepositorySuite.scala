package taskforce.project

import cats.effect.IO
import cats.implicits._
import org.scalacheck.effect.PropF
import taskforce.project.arbitraries._
import taskforce.BasicRepositorySuite
import taskforce.authentication.UserId

class ProjectRepositorySuite extends BasicRepositorySuite {

  var projectRepo: IO[ProjectRepository[IO]] = null
  val userID                                 = UserId(userIdUUID)

  override def beforeAll(): Unit = {
    super.beforeAll()
    projectRepo = LiveProjectRepository.make[IO](xa)

  }

  test("Project Creation test #1") {
    PropF.forAllF { (p: ProjectName) =>
      for {
        repo      <- projectRepo
        allBefore <- repo.list
        _         <- repo.create(p, userID)
        allAfter  <- repo.list
      } yield assertEquals((allBefore.size, allAfter.filter(_.name == p).size, allAfter.size), (2, 1, 3))
    }
  }

  test("created project can be retrieved") {
    PropF.forAllF { (p: ProjectName) =>
      for {
        repo          <- projectRepo
        createdEither <- repo.create(p, userID)
        created       <- IO.fromEither(createdEither)
        retrieved     <- repo.find(created.id)
      } yield assertEquals(created.some, retrieved)
    }
  }

  test("Rename for existing name will fail") {
    PropF.forAllF { (p1: ProjectName, p2: ProjectName) =>
      for {
        repo           <- projectRepo
        createdEither1 <- repo.create(p1, userID)
        _              <- repo.create(p2, userID)
        created1       <- IO.fromEither(createdEither1)
        result         <- repo.update(created1.id, p2)
      } yield assert(result.isLeft)
    }
  }

  test("Project after deletion has 'deleted' field set") {
    PropF.forAllF { (p1: ProjectName) =>
      for {
        repo          <- projectRepo
        createdEither <- repo.create(p1, userID)
        created       <- IO.fromEither(createdEither)
        _             <- repo.delete(created.id)
        retrieved     <- repo.find(created.id)
      } yield assert(clue(retrieved.flatMap(_.deleted).isDefined))
    }

  }

}
