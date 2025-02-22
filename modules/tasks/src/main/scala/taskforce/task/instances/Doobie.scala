package taskforce.task.instances

import doobie.util.meta.Meta
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric
import eu.timepit.refined.types.string
import io.getquill.doobie.DoobieContext
import io.getquill.{MappedEncoding, NamingStrategy, PluralizedTableNames, SnakeCase, querySchema, quote}
import taskforce.common.{NewTypeDoobieMeta, NewTypeQuillInstances}
import taskforce.task.{Task, TaskDuration}

import java.time.Duration

trait Doobie extends NewTypeDoobieMeta with NewTypeQuillInstances {

  val ctx =
    new DoobieContext.Postgres(NamingStrategy(PluralizedTableNames, SnakeCase))
  import ctx.*

  inline def taskQuery = quote {
    querySchema[Task]("tasks", _.created -> "started")
  }

  implicit val decodePositiveInt: MappedEncoding[Int, Refined[Int, numeric.Positive]] =
    MappedEncoding[Int, Int Refined numeric.Positive](Refined.unsafeApply(_))
  implicit val encodePositiveInt: MappedEncoding[Refined[Int, numeric.Positive], Int] =
    MappedEncoding[Int Refined numeric.Positive, Int](_.value)

  implicit val decodeNonEmptyString: MappedEncoding[String, string.NonEmptyString] =
    MappedEncoding[String, string.NonEmptyString](Refined.unsafeApply(_))
  implicit val encodeNonEmptyString: MappedEncoding[string.NonEmptyString, String] =
    MappedEncoding[string.NonEmptyString, String](_.value)

  implicit val taskDurationMeta: Meta[TaskDuration] =
    Meta[Long].imap(x => TaskDuration(Duration.ofMinutes(x)))(x => x.value.toMinutes)

  implicit val decodeTaskDuration: MappedEncoding[Long, TaskDuration] =
    MappedEncoding[Long, TaskDuration](long => TaskDuration(Duration.ofMinutes(long)))
  implicit val encodeTimeDuration: MappedEncoding[TaskDuration, Long] =
    MappedEncoding[TaskDuration, Long](_.value.toMinutes)

}
