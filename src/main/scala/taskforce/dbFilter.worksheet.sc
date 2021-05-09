import eu.timepit.refined.api.Refined
import taskforce.repos.LiveFilterRepository
import taskforce.model._
import java.time.LocalDateTime
import taskforce.repos.LiveProjectRepository
import java.util.UUID
import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts

import cats._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.ExecutionContexts
import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.collection._
import eu.timepit.refined.numeric._
import fs2.Stream
import io.circe.Json
import io.circe.parser._
import io.circe.syntax._
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import taskforce.model._
import taskforce.repos.{LiveFilterRepository, LiveProjectRepository}

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
import y._

val f = Filter(FilterId(UUID.randomUUID()), List(In(List("project 2"))))

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
