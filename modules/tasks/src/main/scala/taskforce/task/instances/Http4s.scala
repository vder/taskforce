package taskforce.task.instances

import org.http4s.EntityEncoder
import org.http4s.circe._
import taskforce.task.Task
import taskforce.task.NewTask

trait Http4s[F[_]] extends Circe {

  implicit val taskEntityEncoder: EntityEncoder[F, Task]       = jsonEncoderOf[F, Task]
  implicit val newTaskEntityEncoder: EntityEncoder[F, NewTask] = jsonEncoderOf[F, NewTask]

}
