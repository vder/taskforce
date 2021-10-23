package taskforce.project.instances

import doobie.util.meta.Meta
import java.time.Duration
import taskforce.project.{ProjectId, TotalTime}

trait Doobie {
  implicit val projectIdMeta: Meta[ProjectId] =
    Meta[Long].imap(ProjectId.apply)(_.value)

  implicit val totalTimeDurationMeta: Meta[TotalTime] =
    Meta[Long].imap(x => TotalTime(Duration.ofMinutes(x)))(x => x.value.toMinutes())
}
