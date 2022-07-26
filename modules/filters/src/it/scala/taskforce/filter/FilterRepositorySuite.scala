package taskforce.filter

import cats.effect.IO
import cats.implicits._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.collection._
import eu.timepit.refined.numeric.Positive
import java.time.LocalDateTime
import org.scalacheck.effect.PropF
import arbitraries._
import taskforce.BasicRepositorySuite

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
        filter = Filter(f, List(In(List(Refined.unsafeApply[String, NonEmpty]("project 1")))))
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
            .execute(
              filter,
              SortBy(CreatedDate, Desc).some,
              page
            )
            .compile
            .toList
        rowsDeleted <-
          fRepo
            .execute(
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
            .execute(
              filter,
              SortBy(CreatedDate, Desc).some,
              page
            )
            .compile
            .toList
        rowsDeleted <-
          fRepo
            .execute(
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
            .execute(
              filter,
              SortBy(CreatedDate, Desc).some,
              page
            )
            .compile
            .toList
        rowsDeleted <-
          fRepo
            .execute(
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
