package taskforce.filter

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.types.string.NonEmptyString
import java.time.format.DateTimeFormatter
import java.time.LocalDate

import org.scalacheck.Gen
import taskforce.task.generators._
import taskforce.filter.model._
import taskforce.project.generators._
import java.time.ZoneOffset
import java.time.Instant


object generators {

  val nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  def instantGen: Gen[Instant] =
    for {
      minutes <- Gen.chooseNum(0, 1000000000)
    } yield LocalDate
      .parse("2000.01.01", DateTimeFormatter.ofPattern("yyyy.MM.dd"))
      .atStartOfDay()
      .plusMinutes(minutes.toLong)
      .toInstant(ZoneOffset.UTC)
  val operatorGen: Gen[Operator] =
    Gen.oneOf(List(Operator.Eq, Operator.Gt, Operator.Gteq, Operator.Lteq, Operator.Lt))

  val statusGen: Gen[Status] = Gen.oneOf(List(Status.Active, Status.Deactive, Status.All))

  val inGen: Gen[Criteria.In] =
    Gen
      .listOf(nonEmptyStringGen.map[NonEmptyString](Refined.unsafeApply))
      .map(Criteria.In.apply)

  val taskCreatedGen: Gen[Criteria.TaskCreatedDate] = for {
    op   <- operatorGen
    date <- instantGen
  } yield Criteria.TaskCreatedDate(op, date)

  val stateGen = statusGen.map(Criteria.State.apply)

  val conditionsGen: Gen[List[Criteria]] = for {
    i <- inGen
    c <- taskCreatedGen
    s <- stateGen
  } yield List(i, c, s)

  val filterIdGen: Gen[FilterId] =
    Gen.uuid.map(FilterId.apply)

  val newFilterGen = conditionsGen.map(NewFilter.apply)

  val filterGen = for {
    c  <- conditionsGen
    id <- filterIdGen
  } yield Filter(id, c)

  val pageSizeGen = for {
    s <- Gen.chooseNum(1, 100).map(Refined.unsafeApply[Int, Positive])
  } yield PageSize(s)

  val pageNoGen = for {
    i <- Gen.chooseNum(1, 100).map(Refined.unsafeApply[Int, Positive])
  } yield PageNo(i)

  val pageGen = for {
    size <- pageSizeGen
    no   <- pageNoGen
  } yield Page(no, size)

  val sortByGen = for {
    field <- Gen.oneOf(List(Field.CreatedDate, Field.UpdatedDate))
    order <- Gen.oneOf(List(Order.Asc, Order.Desc))
  } yield SortBy(field, order)

  val rowGen = for {
    p <- projectGen
    t <- taskGen
  } yield FilterResultRow.fromTuple((p, Some(t)))
}
