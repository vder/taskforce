package taskforce.project

import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string.NonEmptyString
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
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
      name      <- newProjectGen
      userId    <- userIdGen
      created   <- localDateTimeGen
    } yield Project(projectId, name, userId, CreationDate(created), None)

}
