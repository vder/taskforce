import eu.timepit.refined.api.Refined
import taskforce.repository.LiveFilterRepository
import taskforce.model._
import java.time.LocalDateTime
import taskforce.repository.LiveProjectRepository
import java.util.UUID
import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.effect._
import cats.implicits._
import doobie.postgres.implicits._
import java.time.Duration
import eu.timepit.refined._
import eu.timepit.refined.types.string.NonEmptyString
import eu.timepit.refined.numeric._
import eu.timepit.refined.auto._
import eu.timepit.refined.collection._
import io.circe.syntax._
import io.circe.Json
import io.circe.parser._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import fs2.Stream

implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver",                      // driver classname
  "jdbc:postgresql://localhost:54340/exchange", // connect URL
  "vder",                                       // username
  "gordon",                                     // password
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
  .option      // Stream[ConnectionIO, String]
  .quick       // IO[Unit]
  .unsafeRunSync()

val userId =
  UserId(UUID.fromString("5260ca29-a70b-494e-a3d6-55374a3b0a04"))

sql"select id from users where id = ${userId.id}"
  .query[UUID]
  .option
  .quick // IO[Unit]
  .unsafeRunSync()

val opt: Option[Int] = Some(1)

val opt1: Option[Int] = None

opt.attempt
opt1.attempt

println("Sfs")

val d = LocalDateTime.now()

val dur: Duration = Duration.ofSeconds(10)

d.plus(dur)

implicit val td = Get[Long].tmap(x => TaskDuration(Duration.ofSeconds(x)))

// final case class Task(
//     id: TaskId,
//     projectId: ProjectId,
//     owner: UserId,
//     created: LocalDateTime,
//     duration: TaskDuration,
//     volume: Option[Int Refined Positive],
//     deleted: Option[LocalDateTime],
//     comment: Option[NonEmptyString]
// )

val task = Task(
  TaskId(UUID.randomUUID()),
  ProjectId(10),
  TaskDuration(Duration.ofHours(1L)),
  None,
  Some(refineMV[NonEmpty]("comment"))
)

val newProject = NewProject(refineMV("Project name 2"))

val createProject = for {
  db      <- LiveProjectRepository.make[IO](xa)
  project <- db.createProject(newProject, userId)
} yield project

createProject.unsafeRunSync()

task.asJson.noSpaces

userId.asJson.noSpaces

val z = parse("""{
    "projectId": 5}""").toOption.get

z.noSpaces

val f = Filter(
  FilterId(UUID.randomUUID()),
  List(
    In(List(refineMV[NonEmpty]("Git project22333"), refineMV("bbbb"))),
    State(All)
  )
)

val testRun2 = for {
  db   <- Stream.eval(LiveFilterRepository.make[IO](xa))
  rows <- db.getRows(f)
} yield rows

//testRun2.compile.toList.unsafeRunSync()

val testRun = for {
  db     <- LiveFilterRepository.make[IO](xa)
  _      <- db.createFilter(f)
  filter <- db.getFilter(f.id)
} yield filter

testRun.unsafeRunSync()

case class Sort(s: String)
object Sort {
  implicit val sortPut: Put[Sort] =
    doobie.util.Put[String].contramap(x => s" order by $x")
}

val sqlFr = fr"select 1 ${Sort("ordering")}"

sealed trait Field
final case object CreatedDate extends Field
final case object UpdatedDate extends Field

sealed trait Order
final case object Asc  extends Order
final case object Desc extends Order

final case class SortBy(field: Field, order: Order)

s"${CreatedDate}"
