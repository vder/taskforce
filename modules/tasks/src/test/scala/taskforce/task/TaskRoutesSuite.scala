package taskforce.task

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import java.time.{Duration}
import java.util.UUID
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.scalacheck.effect.PropF
import taskforce.HttpTestSuite
import arbitraries._
import taskforce.authentication.UserId
import taskforce.common.{ErrorMessage, LiveHttpErrorHandler}
import taskforce.common.CreationDate
import taskforce.task.instances.Circe
import java.time.Instant

class TasksRoutesSuite extends HttpTestSuite with Circe {

  implicit def encodeNewTask: EntityEncoder[IO, NewTask] = jsonEncoderOf

  def authMiddleware: AuthMiddleware[IO, UserId] =
    AuthMiddleware(Kleisli.pure(UserId(UUID.randomUUID())))

  def authMiddleware(userId: UserId): AuthMiddleware[IO, UserId] =
    AuthMiddleware(Kleisli.pure(userId))

  val errHandler = LiveHttpErrorHandler[IO]

  val currentTime = Instant.now()

  val uri = uri"api/v1/projects/"

  test("cannot delete not other's task") {
    PropF.forAllF { (t: Task, u: UserId) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes = TaskRoutes
        .make[IO](authMiddleware(u), TaskService.make(taskRepo))
        .routes(errHandler)

      DELETE(
        Uri.unsafeFromString(
          s"api/v1/projects/${t.projectId.value}/tasks/${t.id.value}"
        )
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Forbidden,
          ErrorMessage("BASIC-003", "User is not an owner of the resource")
        )
      }
    }
  }

  test("cannot add overlapping tasks #1") {
    PropF.forAllF { (t: Task) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes = TaskRoutes
        .make[IO](authMiddleware(t.author), TaskService.make(taskRepo))
        .routes(errHandler)
      val newTask = NewTask(t.created.some, t.duration, None, None)

      POST(
        newTask,
        Uri.unsafeFromString(s"api/v1/projects/${t.projectId.value}/tasks")
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ErrorMessage(
            "TASK-002",
            "Reporter already logged task in a given time"
          )
        )
      }
    }
  }

  test("cannot add overlapping tasks #2") {
    PropF.forAllF { (t: Task) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes = TaskRoutes
        .make[IO](authMiddleware(t.author), TaskService.make(taskRepo))
        .routes(errHandler)
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
        Uri.unsafeFromString(s"api/v1/projects/${t.projectId.value}/tasks")
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ErrorMessage(
            "TASK-002",
            "Reporter already logged task in a given time"
          )
        )
      }
    }
  }

  test("cannot add overlapping tasks #3") {
    PropF.forAllF { (t: Task) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes = TaskRoutes
        .make[IO](authMiddleware(t.author), TaskService.make(taskRepo))
        .routes(errHandler)
      val newTask =
        NewTask(
          CreationDate(t.created.value.minus(Duration.ofMinutes(10))).some,
          TaskDuration(Duration.ofMinutes(11L)),
          None,
          None
        )

      POST(
        newTask,
        Uri.unsafeFromString(s"api/v1/projects/${t.projectId.value}/tasks")
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ErrorMessage(
            "TASK-002",
            "Reporter already logged task in a given time"
          )
        )
      }
    }
  }

  test("different users can add overlapping tasks") {
    PropF.forAllF { (t: Task, u: UserId) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes = TaskRoutes
        .make[IO](authMiddleware(u), TaskService.make(taskRepo))
        .routes(errHandler)
      val newTask = NewTask(t.created.some, t.duration, None, None)

      POST(
        newTask,
        Uri.unsafeFromString(s"api/v1/projects/${t.projectId.value}/tasks")
      ).pure[IO].flatMap { req =>
        assertHttpStatus(routes, req)(Status.Created)
      }
    }
  }

  test("for unknown ids notFouns err is served") {
    PropF.forAllF { (taskId: TaskId, projectId: ProjectId, u: UserId) =>
      val taskRepo = new TestTaskRepository(List())
      val routes = TaskRoutes
        .make[IO](authMiddleware(u), TaskService.make(taskRepo))
        .routes(errHandler)

      GET(
        Uri.unsafeFromString(
          s"api/v1/projects/${projectId.value}/tasks/${taskId.value}"
        )
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.NotFound,
          ErrorMessage(
            "BASIC-001",
            s"resource with given id ${taskId.value} does not exist"
          )
        )
      }
    }
  }

}
