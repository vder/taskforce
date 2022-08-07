package taskforce.filter

import cats.effect.IO
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.collection._
import eu.timepit.refined.numeric.Positive
import org.scalacheck.effect.PropF
import arbitraries._
import taskforce.BasicRepositorySuite
import java.time.Instant
import taskforce.filter.model._

class FilterRepositorySuite extends BasicRepositorySuite {

  var filterRepo: IO[FilterRepository[IO]] = null

  override def beforeAll(): Unit = {
    super.beforeAll()
    filterRepo = FilterRepository.make[IO](xa).pure[IO]
  }

  test("filter by project Name") {
    PropF.forAllF { (f: FilterId, pageSize: PageSize) =>
      for {
        fRepo <- filterRepo
        filter = Filter(f, List(Criteria.In(List(Refined.unsafeApply[String, NonEmpty]("project 1")))))
        rows <-
          fRepo
            .execute(
              filter,
              None,
              Page(
                PageNo(Refined.unsafeApply[Int, Positive](1)),
                pageSize
              )
            )
            .compile
            .toList
      } yield assertEquals(rows.count(_.projectName == "project 1"), pageSize.value.value.min(6))
    }
  }

  test("sorting asc") {
    PropF.forAllF { (f: FilterId) =>
      for {
        fRepo <- filterRepo
        filter = Filter(f, List(Criteria.In(List(Refined.unsafeApply[String, NonEmpty]("project 1")))))
        page = Page(
          PageNo(Refined.unsafeApply[Int, Positive](1)),
          PageSize(Refined.unsafeApply[Int, Positive](40))
        )
        rowsUnsorted <-
          fRepo
            .execute(
              filter,
              None,
              page
            )
            .compile
            .toList
        rowsSorted <-
          fRepo
            .execute(
              filter,
              SortBy(Field.CreatedDate, Order.Asc).some,
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
        filter = Filter(f, List(Criteria.State(Status.All)))
        page = Page(
          PageNo(Refined.unsafeApply[Int, Positive](1)),
          PageSize(Refined.unsafeApply[Int, Positive](40))
        )
        rowsUnsorted <-
          fRepo
            .execute(
              filter,
              None,
              page
            )
            .compile
            .toList
        rowsSorted <-
          fRepo
            .execute(
              filter,
              SortBy(Field.CreatedDate, Order.Desc).some,
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
        filter = Filter(f, List(Criteria.State(Status.All)))
        page = Page(
          PageNo(Refined.unsafeApply[Int, Positive](1)),
          PageSize(Refined.unsafeApply[Int, Positive](40))
        )
        allRows <-
          fRepo
            .execute(
              filter,
              SortBy(Field.CreatedDate, Order.Desc).some,
              page
            )
            .compile
            .toList
        rowsDeleted <-
          fRepo
            .execute(
              Filter(f, List(Criteria.State(Status.Deactive))),
              SortBy(Field.CreatedDate, Order.Desc).some,
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
        filter = Filter(f, List(Criteria.State(Status.All)))
        page = Page(
          PageNo(Refined.unsafeApply[Int, Positive](1)),
          PageSize(Refined.unsafeApply[Int, Positive](40))
        )
        allRows <-
          fRepo
            .execute(
              filter,
              SortBy(Field.CreatedDate, Order.Desc).some,
              page
            )
            .compile
            .toList
        rowsDeleted <-
          fRepo
            .execute(
              Filter(f, List(Criteria.State(Status.Active))),
              SortBy(Field.CreatedDate, Order.Desc).some,
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
        filter   = Filter(f, List(Criteria.State(Status.All)))
        fromDate = Instant.parse("0004-12-03T10:15:30Z")
        toDate   = Instant.parse("0004-12-03T10:15:30Z")
        page = Page(
          PageNo(Refined.unsafeApply[Int, Positive](1)),
          PageSize(Refined.unsafeApply[Int, Positive](40))
        )
        allRows <-
          fRepo
            .execute(
              filter,
              SortBy(Field.CreatedDate, Order.Desc).some,
              page
            )
            .compile
            .toList
        rowsDeleted <-
          fRepo
            .execute(
              Filter(
                f,
                List(Criteria.TaskCreatedDate(Operator.Gt, fromDate), Criteria.TaskCreatedDate(Operator.Lt, toDate))
              ),
              SortBy(Field.CreatedDate, Order.Desc).some,
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
