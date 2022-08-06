package taskforce.project.instances


import sttp.tapir.Schema
import taskforce.project.Project
import taskforce.common.instances.{TapirCodecs => CommonTapirCodecs}

trait TapirCodecs extends CommonTapirCodecs {

  implicit val sProject: Schema[Project] = Schema.derived

}
