package taskforce.task.instances


import sttp.tapir.Schema
import taskforce.task.Task
import taskforce.common.instances.{TapirCodecs => CommonTapirCodecs}
import taskforce.task.NewTask

trait TapirCodecs extends CommonTapirCodecs  {

  implicit val taskSchema: Schema[Task] = Schema.derived
  implicit val newTaskSchema: Schema[NewTask] = Schema.derived

}
