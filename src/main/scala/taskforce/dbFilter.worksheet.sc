import eu.timepit.refined.api.Refined

import cats.effect._
import cats.implicits._
import doobie._
import doobie.util.ExecutionContexts
import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._
import io.circe.syntax._
import java.util.UUID
import taskforce.model._
import taskforce.repos.LiveFilterRepository

implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

val xa = Transactor.fromDriverManager[IO](
  "org.postgresql.Driver",                       // driver classname
  "jdbc:postgresql://localhost:54340/task_test", // connect URL
  "vder",                                        // username
  "gordon",                                      // password
  Blocker.liftExecutionContext(
    ExecutionContexts.synchronous
  ) // just for testing
)

val y = xa.yolo // a stable reference is required
//import y._

val f = Filter(FilterId(UUID.randomUUID()), List(In(List("project 2"))))

f.asJson.noSpaces

val runFilter = for {
  db <- LiveFilterRepository.make[IO](xa)
  rows <-
    db.getRows(
      f,
      None,
      Page(
        PageNo(Refined.unsafeApply[Int, Positive](1)),
        PageSize(Refined.unsafeApply[Int, Positive](10))
      )
    ).compile
      .toList
} yield rows

val runFilter2 = for {
  db <- LiveFilterRepository.make[IO](xa)
  rows <-
    db.getRows(
      f,
      SortBy(CreatedDate, Asc).some,
      Page(
        PageNo(Refined.unsafeApply[Int, Positive](1)),
        PageSize(Refined.unsafeApply[Int, Positive](10))
      )
    ).compile
      .toList
} yield rows
println(100)

val result = runFilter.unsafeRunSync()

val result2 = runFilter.unsafeRunSync()

result.foreach(println)

println(result.size)

result.sortBy(_.taskCreated)

NewFilter(List(In(List("project 2")))).asJson.noSpaces

NewFilter(List(In(List("project 2")))).asJson.noSpaces

NewFilter(List(State(Active), In(List("project 2")))).asJson.noSpaces

State(Active).asJson.noSpaces

List(State(Active), In(List("project 2"))).asJson.noSpaces
