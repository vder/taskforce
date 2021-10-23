package taskforce.task.instances

import doobie.util.meta.Meta
import taskforce.task.TaskDuration
import java.time.Duration

trait Doobie extends taskforce.project.instances.Doobie {
  implicit val taskDurationMeta: Meta[TaskDuration] =
    Meta[Long].imap(x => TaskDuration(Duration.ofMinutes(x)))(x => x.value.toMinutes())
}
