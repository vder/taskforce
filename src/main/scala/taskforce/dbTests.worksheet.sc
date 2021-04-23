import taskforce.model.domain
import java.util.UUID
import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import fs2.Stream
import doobie.postgres._
import doobie.postgres.implicits._

implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver", // driver classname
  "jdbc:postgresql://localhost:54340/exchange", // connect URL
  "vder", // username
  "gordon", // password
  Blocker.liftExecutionContext(
    ExecutionContexts.synchronous
  ) // just for testing
)

val y = xa.yolo // a stable reference is required
import y._

// implicit val uuidMeta: Meta[UUID] =
//   Meta[String].imap[UUID](UUID.fromString)(_.toString)

sql"select id from users where id ='5260ca29-a70b-494e-a3d6-55374a3b0a04'"
  .query[UUID] // Query0[String]
  .option // Stream[ConnectionIO, String]
  .quick // IO[Unit]
  .unsafeRunSync()

val userId =
  domain.UserId(UUID.fromString("5260ca29-a70b-494e-a3d6-55374a3b0a04"))

sql"select id from users where id = ${userId.id}"
  .query[UUID]
  .option
  .quick // IO[Unit]
  .unsafeRunSync()
