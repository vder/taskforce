package taskforce

import cats.syntax.option._
import eu.timepit.refined._
import eu.timepit.refined.collection._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.types.string.NonEmptyString
import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDate, LocalDateTime}
import org.scalacheck.Gen
import taskforce.authentication.UserId
import taskforce.filter._
import taskforce.project._
import taskforce.task._
import taskforce.common.CreationDate
import taskforce.common.DeletionDate
object generators {

  val nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  val projectIdGen: Gen[ProjectId] =
    Gen.chooseNum[Long](0, 10000).map(ProjectId.apply)

  val userIdGen: Gen[UserId] =
    Gen.uuid.map(UserId.apply)

  val taskIdGen: Gen[TaskId] =
    Gen.uuid.map(TaskId.apply)

  val taskDurationGen: Gen[TaskDuration] =
    Gen.chooseNum[Long](10, 1000).map(x => TaskDuration(Duration.ofMinutes(x)))

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

  val projectGen: Gen[Project] =
    for {
      projectId <- projectIdGen
      name <- newProjectGen
      userId <- userIdGen
      created <- localDateTimeGen
    } yield Project(projectId, name, userId, CreationDate(created), None)

  val taskVolumeGen: Gen[TaskVolume] =
    Gen
      .posNum[Int]
      .map(Refined.unsafeApply[Int, Positive])
      .map(TaskVolume.apply)

  val taskCommentGen: Gen[Option[TaskComment]] =
    Gen.alphaStr
      .map(refineV[NonEmpty](_).toOption)
      .map(_.map(TaskComment.apply))

  val taskGen: Gen[Task] =
    for {
      id <- taskIdGen
      projectId <- projectIdGen
      author <- userIdGen
      created <- localDateTimeGen
      duration <- taskDurationGen
      volume <- taskVolumeGen
      comment <- taskCommentGen
    } yield Task(
      id,
      projectId,
      author,
      CreationDate(created),
      duration,
      volume.some,
      None,
      comment
    )

  val newTaskGen: Gen[NewTask] =
    for {
      created <- creationDateTimeGen
      duration <- taskDurationGen
      volume <- taskVolumeGen
      comment <- taskCommentGen
    } yield NewTask(created.some, duration, volume.some, comment)

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
