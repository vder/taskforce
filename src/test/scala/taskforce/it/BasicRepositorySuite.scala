package taskforce.it

import pureconfig.ConfigSource
import cats.effect.Blocker
import cats.effect.IO
import doobie.util.ExecutionContexts
import doobie.util.transactor
import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.collection._
import eu.timepit.refined.string._
import java.util.UUID
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.flywaydb.core.Flyway
import taskforce.config.DatabaseConfig
import taskforce.model.UserId

trait BasicRepositorySuite extends CatsEffectSuite with ScalaCheckEffectSuite {

  var db: DatabaseConfig                      = null
  var flyway: Flyway                          = null
  var xa: transactor.Transactor.Aux[IO, Unit] = null
  val userID                                  = UserId(UUID.fromString("5260ca29-a70b-494e-a3d6-55374a3b0a04"))

  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(1)

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

    xa = transactor.Transactor.fromDriverManager[IO](
      db.driver,
      db.url,
      db.user,
      db.pass,
      Blocker.liftExecutionContext(ExecutionContexts.synchronous) // just for testing
    )

  }

  override def beforeEach(context: BeforeEach): Unit = {
    val flyway = Flyway.configure().dataSource(db.url.value, db.user.value, db.pass.value).load()
    flyway.clean()
    flyway.migrate()
  }

}
