package taskforce.routes

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import java.time.LocalDateTime
import java.util.UUID
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.scalacheck.effect.PropF
import suite.HttpTestSuite
import taskforce.arbitraries._
import taskforce.http.LiveHttpErrorHandler
import taskforce.http.TaskRoutes
import taskforce.model.NewTask
import taskforce.model.Task
import taskforce.model.UserId
import taskforce.model.errors._
import taskforce.repository.TestTaskRepository
import java.time.Duration
import taskforce.model.TaskDuration
class TasksRoutesSuite extends HttpTestSuite {
  // implicit def decodeNewProduct: EntityDecoder[IO, NewProject] = jsonOf
  implicit def encodeNewProduct: EntityEncoder[IO, NewTask] = jsonEncoderOf

  val errHandler = LiveHttpErrorHandler.apply[IO]
  def authMiddleware: AuthMiddleware[IO, UserId] =
    AuthMiddleware(Kleisli.pure(UserId(UUID.randomUUID())))

  def authMiddleware(userId: UserId): AuthMiddleware[IO, UserId] =
    AuthMiddleware(Kleisli.pure(userId))

  val currentTime = LocalDateTime.now()

  val uri = uri"api/v1/projects/"

  test("cannot delete not other's task") {
    PropF.forAllF { (t: Task, u: UserId) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes   = errHandler.handle(new TaskRoutes[IO](authMiddleware(u), taskRepo).routes)

      DELETE(Uri.unsafeFromString(s"api/v1/projects/${t.projectId.value}/tasks/${t.id.value}")).flatMap { req =>
        assertHttp(routes, req)(Status.Forbidden, ErrorMessage("001", "User is not an owner of the resource"))
      }
    }
  }

  test("cannot add overlapping tasks #1") {
    PropF.forAllF { (t: Task) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes   = errHandler.handle(new TaskRoutes[IO](authMiddleware(t.author), taskRepo).routes)
      val newTask  = NewTask(t.projectId, t.created.some, t.duration, None, None)

      POST(newTask, Uri.unsafeFromString(s"api/v1/projects/${t.projectId.value}/tasks")).flatMap { req =>
        assertHttp(routes, req)(Status.Conflict, ErrorMessage("005", "Reporter already logged task in a given time"))
      }
    }
  }

  test("cannot add overlapping tasks #2") {
    PropF.forAllF { (t: Task) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes   = errHandler.handle(new TaskRoutes[IO](authMiddleware(t.author), taskRepo).routes)
      val newTask =
        NewTask(t.projectId, t.created.plus(t.duration.value).minus(Duration.ofMinutes(1)).some, t.duration, None, None)

      POST(newTask, Uri.unsafeFromString(s"api/v1/projects/${t.projectId.value}/tasks")).flatMap { req =>
        assertHttp(routes, req)(Status.Conflict, ErrorMessage("005", "Reporter already logged task in a given time"))
      }
    }
  }

  test("cannot add overlapping tasks #3") {
    PropF.forAllF { (t: Task) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes   = errHandler.handle(new TaskRoutes[IO](authMiddleware(t.author), taskRepo).routes)
      val newTask =
        NewTask(
          t.projectId,
          t.created.minus(Duration.ofMinutes(10)).some,
          TaskDuration(Duration.ofMinutes(11L)),
          None,
          None
        )

      POST(newTask, Uri.unsafeFromString(s"api/v1/projects/${t.projectId.value}/tasks")).flatMap { req =>
        assertHttp(routes, req)(Status.Conflict, ErrorMessage("005", "Reporter already logged task in a given time"))
      }
    }
  }

  test("different users can add overlapping tasks") {
    PropF.forAllF { (t: Task, u: UserId) =>
      val taskRepo = new TestTaskRepository(List(t))
      val routes   = errHandler.handle(new TaskRoutes[IO](authMiddleware(u), taskRepo).routes)
      val newTask  = NewTask(t.projectId, t.created.some, t.duration, None, None)

      POST(newTask, Uri.unsafeFromString(s"api/v1/projects/${t.projectId.value}/tasks")).flatMap { req =>
        assertHttpStatus(routes, req)(Status.Created)
      }
    }
  }

}
