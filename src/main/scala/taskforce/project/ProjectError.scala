package taskforce.project

import scala.util.control.NoStackTrace

sealed trait ProjectError extends NoStackTrace

final case class DuplicateProjectNameError(newProject: NewProject) extends ProjectError
