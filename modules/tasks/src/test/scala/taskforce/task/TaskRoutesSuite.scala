package taskforce.task

import cats.effect.IO
import cats.implicits._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import io.circe.generic.auto._
import org.scalacheck.effect.PropF
import sttp.tapir.Endpoint
import sttp.tapir.server.PartialServerEndpoint
import taskforce.HttpTestSuite
import taskforce.authentication.Authenticator
import taskforce.authentication.UserId
//import taskforce.common.CreationDate
//import taskforce.common.ErrorMessage
import taskforce.common.ResponseError
import taskforce.common.instances.Http4s
import taskforce.task.instances.Circe

//import java.time.Duration

import arbitraries._
import org.http4s.headers.Authorization
import taskforce.common.CreationDate
import java.time.Duration

class TasksRoutesSuite extends HttpTestSuite with Circe with Http4s[IO] {

  implicit def encodeNewTask: EntityEncoder[IO, NewTask] = jsonEncoderOf

  private def testAuthenticator(userId: UserId) = new Authenticator[IO] {
    def secureEndpoints[SECURITY_INPUT, INPUT, OUTPUT](
        endpoints: Endpoint[String, INPUT, ResponseError, OUTPUT, Any]
    ): PartialServerEndpoint[String, UserId, INPUT, ResponseError, OUTPUT, Any, IO] =
      endpoints
        .serverSecurityLogic { _ => userId.asRight[ResponseError].pure[IO] }
  }

  private val uri        = uri"api/v1/projects/"
  private val authHeader = Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame"))

  test("cannot delete not other's task") {
    PropF.forAllF { (t: Task, u: UserId) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes = TaskRoutes
        .make[IO](testAuthenticator(u), TaskService.make(taskRepo))
        .routes

      DELETE(
        uri / t.projectId.value / "tasks" / t.id.value,
        authHeader
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Forbidden,
          ResponseError.NotAuthor(s"user $u is not an Author")
        )
      }
    }
  }

  test("cannot add overlapping tasks #1") {
    PropF.forAllF { (t: Task) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes = TaskRoutes
        .make[IO](testAuthenticator(t.author), TaskService.make(taskRepo))
        .routes
      val newTask = NewTask(t.created.some, t.duration, None, None)

      POST(
        newTask,
        uri / t.projectId.value / "tasks",
        authHeader
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ResponseError.WrongPeriodError("Reporter already logged task in a given time")
        )
      }
    }
  }

  test("cannot add overlapping tasks #2") {
    PropF.forAllF { (t: Task) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes = TaskRoutes
        .make[IO](testAuthenticator(t.author), TaskService.make(taskRepo))
        .routes
      val newTask =
        NewTask(
          CreationDate(
            t.created.value.plus(t.duration.value).minus(Duration.ofMinutes(1))
          ).some,
          t.duration,
          None,
          None
        )

      POST(
        newTask,
        uri / t.projectId.value / "tasks",
        authHeader
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ResponseError.WrongPeriodError("Reporter already logged task in a given time")
        )
      }
    }
  }

  test("cannot add overlapping tasks #3") {
    PropF.forAllF { (t: Task) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes = TaskRoutes
        .make[IO](testAuthenticator(t.author), TaskService.make(taskRepo))
        .routes
      val newTask =
        NewTask(
          CreationDate(t.created.value.minus(Duration.ofMinutes(10))).some,
          TaskDuration(Duration.ofMinutes(11L)),
          None,
          None
        )

      POST(
        newTask,
        uri / t.projectId.value / "tasks",
        authHeader
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ResponseError.WrongPeriodError("Reporter already logged task in a given time")
        )
      }
    }
  }

  test("different users can add overlapping tasks") {
    PropF.forAllF { (t: Task, u: UserId) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes = TaskRoutes
        .make[IO](testAuthenticator(u), TaskService.make(taskRepo))
        .routes
      val newTask = NewTask(t.created.some, t.duration, None, None)

      POST(
        newTask,
        uri / t.projectId.value / "tasks",
        authHeader
      ).pure[IO].flatMap { req =>
        assertHttpStatus(routes, req)(Status.Created)
      }
    }
  }

  test("for unknown ids notFound err is served") {
    PropF.forAllF { (taskId: TaskId, projectId: ProjectId, u: UserId) =>
      val taskRepo = new TestTaskRepository(List())
      val routes = TaskRoutes
        .make[IO](testAuthenticator(u), TaskService.make(taskRepo))
        .routes

      GET(
        uri / projectId.value / "tasks" / taskId.value,
        authHeader
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.NotFound,
          ResponseError.NotFound(s"resource ${taskId.value} is not found")
        )
      }
    }
  }

}
