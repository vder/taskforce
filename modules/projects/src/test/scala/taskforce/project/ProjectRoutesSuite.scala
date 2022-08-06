package taskforce.project

import cats.effect.IO
import cats.implicits._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import org.scalacheck.effect.PropF
import taskforce.HttpTestSuite
import taskforce.authentication.UserId
import taskforce.common.ErrorHandler
import taskforce.project.ProjectName
import taskforce.project.instances.Circe
import taskforce.common.instances.Http4s
import java.time.Instant
import taskforce.common.AppError
import taskforce.authentication.Authenticator
import taskforce.common.ResponseError
import io.circe.refined._
import org.http4s.headers.Authorization
import sttp.tapir.Endpoint
import sttp.tapir.server.PartialServerEndpoint
import io.circe.generic.auto._


class ProjectRoutesSuite extends HttpTestSuite with Circe with Http4s[IO] {

  import arbitraries._

  implicit def decodeNewProduct: EntityDecoder[IO, ProjectName] = jsonOf
  implicit def encodeNewProduct: EntityEncoder[IO, ProjectName] = jsonEncoderOf

  val errHandler = ErrorHandler[IO]

  def testAuthenticator(userId: UserId) = new Authenticator[IO] {
    def secureEndpoints[SECURITY_INPUT, INPUT, OUTPUT](
        endpoints: Endpoint[String, INPUT, ResponseError, OUTPUT, Any]
    ): PartialServerEndpoint[String, UserId, INPUT, ResponseError, OUTPUT, Any, IO] =
      endpoints
        .serverSecurityLogic { _ => userId.asRight[ResponseError].pure[IO] }
  }

  val currentTime = Instant.now()

  val uri = uri"api/v1/projects"

  val authHeader =  Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame"))

  test("Cannot rename project to existing same name") {
    PropF.forAllF { (p1: Project, p2: Project) =>
      val projectRepo = new TestProjectRepository(List(p1, p2), currentTime) {
        override def update(id: ProjectId, newProject: ProjectName) =
          AppError.DuplicateProjectName(s"$newProject").asLeft[Project].pure[IO]
      }
      val routes =
        ProjectRoutes.make[IO](testAuthenticator(p1.author), ProjectService.make[IO](projectRepo)).routes
      PUT(
        p2.name,
        Uri.unsafeFromString(s"api/v1/projects/${p1.id.value}"),
        authHeader
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ResponseError.DuplicateProjectName2(s"project name '${p2.name}' already exists")
        )
      }
    }
  }

  test("Cannot create project with the same name") {
    PropF.forAllF { (p1: Project, u: UserId) =>
      val projectRepo = new TestProjectRepository(List(), currentTime) {
        override def create(
            newProject: ProjectName,
            userId: UserId
        ): IO[Either[AppError.DuplicateProjectName, Project]] =
          AppError.DuplicateProjectName(s"$newProject").asLeft[Project].pure[IO]
      }

      val routes = ProjectRoutes.make[IO](testAuthenticator(u), ProjectService.make[IO](projectRepo)).routes

      POST(p1.name, uri, authHeader).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ResponseError.DuplicateProjectName2(s"project name '${p1.name}' already exists")
        )
      }
    }
  }

  test("returns project with given ID") {
    PropF.forAllF { (p: Project, list: List[Project]) =>
      val projectRepo = new TestProjectRepository(p :: list, currentTime)
      val routes      = ProjectRoutes.make[IO](testAuthenticator(p.author), ProjectService.make[IO](projectRepo)).routes

      GET(Uri.unsafeFromString(s"api/v1/projects/${p.id.value}"), authHeader).pure[IO].flatMap { req =>
        assertHttp(routes, req)(Status.Ok, p)
      }
    }
  }

  test("cannot delete others project") {
    PropF.forAllF { (p: Project, u: UserId) =>
      val projectRepo = new TestProjectRepository(List(p), currentTime)
      val routes      = ProjectRoutes.make[IO](testAuthenticator(u), ProjectService.make[IO](projectRepo)).routes

      DELETE(Uri.unsafeFromString(s"api/v1/projects/${p.id.value}"), authHeader).pure[IO].flatMap { req =>
        assertHttp(routes, req)(Status.Forbidden, ResponseError.NotAuthor(s"user $u is not an Author"))
      }
    }
  }
  test("provide valid response where project is not found") {
    PropF.forAllF { (p: ProjectId, u: UserId) =>
      val projectRepo = new TestProjectRepository(List(), currentTime)
      val routes      = ProjectRoutes.make[IO](testAuthenticator(u), ProjectService.make[IO](projectRepo)).routes

      DELETE(Uri.unsafeFromString(s"api/v1/projects/${p.value}"),authHeader).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.NotFound,
          ResponseError.NotFound(s"resource ${p.value} is not Found")
        )
      }
    }
  }

}
