package taskforce.it

import cats.effect.Blocker
import cats.effect.IO
import cats.implicits._
import doobie.util.ExecutionContexts
import doobie.util.transactor
import pureconfig.ConfigSource
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.collection._
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.string._
import java.util.UUID
import java.time.LocalDateTime
import munit.CatsEffectSuite
import munit.ScalaCheckEffectSuite
import org.flywaydb.core.Flyway
import org.scalacheck.effect.PropF
import taskforce.arbitraries.arbFilterIdGen
import taskforce.arbitraries.arbPageSizeGen
import taskforce.config.DatabaseConfig
import taskforce.model._
import taskforce.repos._

class FilterRepositorySuite extends CatsEffectSuite with ScalaCheckEffectSuite {

  var filterRepo: IO[FilterRepository[IO]] = null
  var db: DatabaseConfig                   = null
  var flyway: Flyway                       = null
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
    filterRepo = LiveFilterRepository.make[IO](xa)

  }

  override def beforeEach(context: BeforeEach): Unit = {
    val flyway = Flyway.configure().dataSource(db.url.value, db.user.value, db.pass.value).load()
    flyway.clean()
    flyway.migrate()
  }

  test("filter by project Name") {
    PropF.forAllF { (f: FilterId, pageSize: PageSize) =>
      for {
        fRepo <- filterRepo
        filter = Filter(f, List(In(List(Refined.unsafeApply[String, NonEmpty]("project 1")))))
        rows <-
          fRepo
            .getRows(
              filter,
              None,
              Page(
                PageNo(Refined.unsafeApply[Int, Positive](1)),
                pageSize
              )
            )
            .compile
            .toList
      } yield assertEquals(rows.filter(_.projectName == "project 1").size, pageSize.value.value.min(6))
    }
  }

  test("sorting asc") {
    PropF.forAllF { (f: FilterId) =>
      for {
        fRepo <- filterRepo
        filter = Filter(f, List(In(List(Refined.unsafeApply[String, NonEmpty]("project 1")))))
        page = Page(
          PageNo(Refined.unsafeApply[Int, Positive](1)),
          PageSize(Refined.unsafeApply[Int, Positive](40))
        )
        rowsUnsorted <-
          fRepo
            .getRows(
              filter,
              None,
              page
            )
            .compile
            .toList
        rowsSorted <-
          fRepo
            .getRows(
              filter,
              SortBy(CreatedDate, Asc).some,
              page
            )
            .compile
            .toList
      } yield assertEquals(rowsSorted, rowsUnsorted.sortBy(_.taskCreated))

    }
  }

  test("sorting desc") {
    PropF.forAllF { (f: FilterId) =>
      for {
        fRepo <- filterRepo
        filter = Filter(f, List(State(All)))
        page = Page(
          PageNo(Refined.unsafeApply[Int, Positive](1)),
          PageSize(Refined.unsafeApply[Int, Positive](40))
        )
        rowsUnsorted <-
          fRepo
            .getRows(
              filter,
              None,
              page
            )
            .compile
            .toList
        rowsSorted <-
          fRepo
            .getRows(
              filter,
              SortBy(CreatedDate, Desc).some,
              page
            )
            .compile
            .toList
      } yield assertEquals(rowsSorted, rowsUnsorted.sortBy(_.taskCreated).reverse)

    }
  }

  test("only deleted") {
    PropF.forAllF { (f: FilterId) =>
      for {
        fRepo <- filterRepo
        filter = Filter(f, List(State(All)))
        page = Page(
          PageNo(Refined.unsafeApply[Int, Positive](1)),
          PageSize(Refined.unsafeApply[Int, Positive](40))
        )
        allRows <-
          fRepo
            .getRows(
              filter,
              SortBy(CreatedDate, Desc).some,
              page
            )
            .compile
            .toList
        rowsDeleted <-
          fRepo
            .getRows(
              Filter(f, List(State(Deactive))),
              SortBy(CreatedDate, Desc).some,
              page
            )
            .compile
            .toList
      } yield assertEquals(rowsDeleted, allRows.filter(_.taskDeleted.isDefined))
    }
  }

  test("only active") {
    PropF.forAllF { (f: FilterId) =>
      for {
        fRepo <- filterRepo
        filter = Filter(f, List(State(All)))
        page = Page(
          PageNo(Refined.unsafeApply[Int, Positive](1)),
          PageSize(Refined.unsafeApply[Int, Positive](40))
        )
        allRows <-
          fRepo
            .getRows(
              filter,
              SortBy(CreatedDate, Desc).some,
              page
            )
            .compile
            .toList
        rowsDeleted <-
          fRepo
            .getRows(
              Filter(f, List(State(Active))),
              SortBy(CreatedDate, Desc).some,
              page
            )
            .compile
            .toList
      } yield assertEquals(rowsDeleted, allRows.filter(_.taskDeleted.isEmpty))
    }
  }

  test("filter by date") {
    PropF.forAllF { (f: FilterId) =>
      for {
        fRepo <- filterRepo
        filter   = Filter(f, List(State(All)))
        fromDate = LocalDateTime.parse("0004-12-03T10:15:30")
        toDate   = LocalDateTime.parse("0004-12-03T10:15:30")
        page = Page(
          PageNo(Refined.unsafeApply[Int, Positive](1)),
          PageSize(Refined.unsafeApply[Int, Positive](40))
        )
        allRows <-
          fRepo
            .getRows(
              filter,
              SortBy(CreatedDate, Desc).some,
              page
            )
            .compile
            .toList
        rowsDeleted <-
          fRepo
            .getRows(
              Filter(f, List(TaskCreatedDate(Gt, fromDate), TaskCreatedDate(Lt, toDate))),
              SortBy(CreatedDate, Desc).some,
              page
            )
            .compile
            .toList
      } yield assertEquals(
        rowsDeleted,
        allRows.filter(_.taskCreated.map(t => t.isAfter(fromDate) && t.isBefore(toDate)).getOrElse(false))
      )
    }
  }

}
