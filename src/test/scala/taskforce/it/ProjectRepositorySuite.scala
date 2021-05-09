package taskforce.it

import munit.CatsEffectSuite
import munit.ScalaCheckEffectSuite
import cats.effect.IO
import cats.implicits._
import org.scalacheck.effect.PropF
import org.flywaydb.core.Flyway
import pureconfig.ConfigSource
import taskforce.config.DatabaseConfig
import doobie.util.transactor
import eu.timepit.refined._
import eu.timepit.refined.string._
import eu.timepit.refined.auto._
import eu.timepit.refined.collection._
import cats.effect.Blocker
import doobie.util.ExecutionContexts
import taskforce.repos.LiveProjectRepository
import taskforce.repos.ProjectRepository
import taskforce.model.UserId
import taskforce.model.NewProject
import taskforce.arbitraries._
import java.util.UUID

class ProjectRepositorySuite extends CatsEffectSuite with ScalaCheckEffectSuite {

  var projectRepo: IO[ProjectRepository[IO]] = null
  var db: DatabaseConfig                     = null
  var flyway: Flyway                         = null
  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(1)

  val userID = UserId(UUID.fromString("5260ca29-a70b-494e-a3d6-55374a3b0a04"))

  override def beforeAll(): Unit = {

    db = ConfigSource.default
      .at("database_test")
      .load[DatabaseConfig]
      .getOrElse(
        DatabaseConfig(
          refineMV[NonEmpty]("org.postgresql.Driver"),
          refineMV[Uri]("jdbc:postgresql://localhost:54340/test"),
          refineMV[NonEmpty]("vder"),
          refineMV[NonEmpty]("gordon")
        )
      )

    flyway = Flyway.configure().dataSource(db.url.value, db.user.value, db.pass.value).load()
    flyway.clean()
    flyway.migrate()

    val xa = transactor.Transactor.fromDriverManager[IO](
      db.driver,
      db.url,
      db.user,
      db.pass,
      Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
    )

    projectRepo = LiveProjectRepository.make[IO](xa)

  }

  override def beforeEach(context: BeforeEach): Unit = {
    val flyway = Flyway.configure().dataSource(db.url.value, db.user.value, db.pass.value).load()
    flyway.clean()
    flyway.migrate()
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
