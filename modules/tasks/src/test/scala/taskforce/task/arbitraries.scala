package taskforce.task

import org.scalacheck.Arbitrary
import generators._
import taskforce.task.ProjectId
import taskforce.authentication.UserId
import taskforce.task.TaskId
import taskforce.task.TaskDuration
import java.time.Instant

object arbitraries {

  implicit def arbNonEmptyStringGen: Arbitrary[String] = Arbitrary(nonEmptyStringGen)
  implicit def arbProjectIdGen: Arbitrary[ProjectId]      = Arbitrary(projectIdGen)
  implicit def arbUserIdGen: Arbitrary[UserId]         = Arbitrary(userIdGen)
  implicit def arbTaskIdGen: Arbitrary[TaskId]         = Arbitrary(taskIdGen)
  implicit def arbTaskDurationGen: Arbitrary[TaskDuration]   = Arbitrary(taskDurationGen)
  implicit def arbInstantGen: Arbitrary[Instant]        = Arbitrary(instantGen)
  implicit def arbTaskGen: Arbitrary[Task]           = Arbitrary(taskGen)
  implicit def arbNewTaskGen: Arbitrary[NewTask]        = Arbitrary(newTaskGen)

}
