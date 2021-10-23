package taskforce

import pureconfig.ConfigSource
import cats.effect.IO
import doobie.util.transactor
import eu.timepit.refined.auto._
import java.util.UUID
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.flywaydb.core.Flyway
import taskforce.config.DatabaseConfig
import taskforce.authentication.UserId

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
      .toOption
      .get

    flyway = Flyway.configure().dataSource(db.url.value, db.user.value, db.pass.value).load()
    flyway.clean()
    flyway.migrate()

    xa = transactor.Transactor.fromDriverManager[IO](
      db.driver,
      db.url,
      db.user,
      db.pass
    )

  }

  override def beforeEach(context: BeforeEach): Unit = {
    val flyway = Flyway.configure().dataSource(db.url.value, db.user.value, db.pass.value).load()
    flyway.clean()
    flyway.migrate()
  }

}
