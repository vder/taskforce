package taskforce


import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.types.string.NonEmptyString
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import org.scalacheck.Gen
import taskforce.authentication.UserId
import taskforce.filter._
import taskforce.project._
import taskforce.task.generators._
import taskforce.project.generators._
import taskforce.common.CreationDate
import taskforce.common.DeletionDate
object generators {

  val nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }


  val userIdGen: Gen[UserId] =
    Gen.uuid.map(UserId.apply)

  val newProjectGen: Gen[ProjectName] =
    nonEmptyStringGen
      .map[NonEmptyString](Refined.unsafeApply)
      .map(ProjectName.apply)

  def creationDateTimeGen: Gen[CreationDate] =
    localDateTimeGen.map(CreationDate.apply)

  def deletionDateTimeGen: Gen[DeletionDate] =
    localDateTimeGen.map(DeletionDate.apply)

  def localDateTimeGen: Gen[LocalDateTime] =
    for {
      minutes <- Gen.chooseNum(0, 1000000000)
    } yield LocalDate
      .parse("2000.01.01", DateTimeFormatter.ofPattern("yyyy.MM.dd"))
      .atStartOfDay()
      .plusMinutes(minutes.toLong)

  val operatorGen: Gen[Operator] = Gen.oneOf(List(Eq, Gt, Gteq, Lteq, Lt))

  val statusGen: Gen[Status] = Gen.oneOf(List(Active, Deactive, All))

  val inGen: Gen[In] =
    Gen
      .listOf(nonEmptyStringGen.map[NonEmptyString](Refined.unsafeApply))
      .map(In.apply)

  val taskCreatedGen: Gen[TaskCreatedDate] = for {
    op <- operatorGen
    date <- localDateTimeGen
  } yield TaskCreatedDate(op, date)

  val stateGen = statusGen.map(State.apply)

  val conditionsGen: Gen[List[Criteria]] = for {
    i <- inGen
    c <- taskCreatedGen
    s <- stateGen
  } yield List(i, c, s)

  val filterIdGen: Gen[FilterId] =
    Gen.uuid.map(FilterId.apply)

  val newFilterGen = conditionsGen.map(NewFilter.apply)

  val filterGen = for {
    c <- conditionsGen
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
    no <- pageNoGen
  } yield Page(no, size)

  val sortByGen = for {
    field <- Gen.oneOf(List(CreatedDate, UpdatedDate))
    order <- Gen.oneOf(List(Asc, Desc))
  } yield SortBy(field, order)

  val rowGen = for {
    p <- projectGen
    t <- taskGen
  } yield FilterResultRow.fromTuple((p, Some(t)))
}
