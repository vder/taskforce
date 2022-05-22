package taskforce.task

import cats.syntax.option._
import eu.timepit.refined._
import eu.timepit.refined.collection._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDate, LocalDateTime}
import org.scalacheck.Gen
import taskforce.authentication.UserId
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
      id        <- taskIdGen
      projectId <- projectIdGen
      author    <- userIdGen
      created   <- localDateTimeGen
      duration  <- taskDurationGen
      volume    <- taskVolumeGen
      comment   <- taskCommentGen
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
      created  <- creationDateTimeGen
      duration <- taskDurationGen
      volume   <- taskVolumeGen
      comment  <- taskCommentGen
    } yield NewTask(created.some, duration, volume.some, comment)

}
