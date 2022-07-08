package taskforce.task.instances

import doobie.util.meta.Meta
import taskforce.task.TaskDuration
import java.time.Duration
import io.getquill.MappedEncoding
import taskforce.common.NewTypeQuillInstances

trait Doobie extends NewTypeQuillInstances {

  implicit val taskDurationMeta: Meta[TaskDuration] =
    Meta[Long].imap(x => TaskDuration(Duration.ofMinutes(x)))(x => x.value.toMinutes)

  implicit val decodeTaskDuration: MappedEncoding[Long, TaskDuration] =
    MappedEncoding[Long, TaskDuration](long => TaskDuration(Duration.ofMinutes(long)))
  implicit val encodeTimeDuration: MappedEncoding[TaskDuration, Long] =
    MappedEncoding[TaskDuration, Long](_.value.toMinutes)

}
