package taskforce.project

import org.scalacheck.Arbitrary
import generators._
import taskforce.project.ProjectId
import taskforce.authentication.UserId
import taskforce.project.ProjectName
import java.time.Instant

object arbitraries {

  implicit def arbNonEmptyStringGen:  Arbitrary[String]  = Arbitrary(nonEmptyStringGen)
  implicit def arbProjectIdGen: Arbitrary[ProjectId]      = Arbitrary(projectIdGen)
  implicit def arbUserIdGen: Arbitrary[UserId]         = Arbitrary(userIdGen)
  implicit def arbNewProjectGen: Arbitrary[ProjectName]     = Arbitrary(newProjectGen)
  implicit def arbInstantGen: Arbitrary[Instant]        = Arbitrary(instantGen)
  implicit def arbProjectGen: Arbitrary[Project]        = Arbitrary(projectGen)

}
