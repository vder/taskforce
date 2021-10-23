package taskforce.project

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
import taskforce.authentication.UserId
import taskforce.common.{ErrorMessage, LiveHttpErrorHandler}

class ProjectRoutesSuite extends HttpTestSuite with instances.Circe {

  implicit def decodeNewProduct: EntityDecoder[IO, NewProject] = jsonOf
  implicit def encodeNewProduct: EntityEncoder[IO, NewProject] = jsonEncoderOf

  val errHandler = LiveHttpErrorHandler[IO]

  def authMiddleware: AuthMiddleware[IO, UserId] =
    AuthMiddleware(Kleisli.pure(UserId(UUID.randomUUID())))

  def authMiddleware(userId: UserId): AuthMiddleware[IO, UserId] =
    AuthMiddleware(Kleisli.pure(userId))

  val currentTime = LocalDateTime.now()

  val uri = uri"api/v1/projects"

  test("Cannot rename project to existing same name") {
    PropF.forAllF { (p1: Project, p2: Project) =>
      val projectRepo = new TestProjectRepository(List(p1, p2), currentTime) {
        override def update(id: ProjectId, newProject: NewProject) =
          DuplicateProjectNameError(newProject).asLeft[Project].pure[IO]
      }
      val routes =
        new ProjectRoutes[IO](authMiddleware(p1.author), new ProjectService[IO](projectRepo)).routes(errHandler)
      PUT(NewProject(p2.name), Uri.unsafeFromString(s"api/v1/projects/${p1.id.value}")).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ErrorMessage("PROJECT-001", s"name given in request: ${p2.name.value} already exists")
        )
      }
    }
  }

  test("Cannot create project with the same name") {
    PropF.forAllF { (p1: Project, u: UserId) =>
      val projectRepo = new TestProjectRepository(List(), currentTime) {
        override def create(
            newProject: NewProject,
            userId: UserId
        ): IO[Either[DuplicateProjectNameError, Project]] =
          DuplicateProjectNameError(newProject).asLeft[Project].pure[IO]
      }

      val routes = new ProjectRoutes[IO](authMiddleware(u), new ProjectService[IO](projectRepo)).routes(errHandler)
      POST(NewProject(p1.name), uri).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ErrorMessage("PROJECT-001", s"name given in request: ${p1.name.value} already exists")
        )
      }
    }
  }

  test("returns project with given ID") {
    PropF.forAllF { (p: Project, list: List[Project]) =>
      val projectRepo = new TestProjectRepository(p :: list, currentTime)
      val routes      = new ProjectRoutes[IO](authMiddleware, new ProjectService[IO](projectRepo)).routes(errHandler)

      GET(Uri.unsafeFromString(s"api/v1/projects/${p.id.value}")).pure[IO].flatMap { req =>
        assertHttp(routes, req)(Status.Ok, p)
      }
    }
  }

  test("cannot delete others project") {
    PropF.forAllF { (p: Project, u: UserId) =>
      val projectRepo = new TestProjectRepository(List(p), currentTime)
      val routes      = new ProjectRoutes[IO](authMiddleware(u), new ProjectService[IO](projectRepo)).routes(errHandler)

      DELETE(Uri.unsafeFromString(s"api/v1/projects/${p.id.value}")).pure[IO].flatMap { req =>
        assertHttp(routes, req)(Status.Forbidden, ErrorMessage("BASIC-003", "User is not an owner of the resource"))
      }
    }
  }
  test("provide valid response where project is not found") {
    PropF.forAllF { (p: ProjectId, u: UserId) =>
      val projectRepo = new TestProjectRepository(List(), currentTime)
      val routes      = new ProjectRoutes[IO](authMiddleware(u), new ProjectService[IO](projectRepo)).routes(errHandler)

      DELETE(Uri.unsafeFromString(s"api/v1/projects/${p.value}")).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.NotFound,
          ErrorMessage(
            "BASIC-001",
            s"resource with given id ${p.value} does not exist"
          )
        )
      }
    }
  }
}