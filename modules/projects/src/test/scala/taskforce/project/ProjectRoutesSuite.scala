package taskforce.project

// import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
// import io.circe.refined._
// import java.util.UUID
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.implicits._
// import org.http4s.server.AuthMiddleware
import org.scalacheck.effect.PropF
import taskforce.HttpTestSuite
import taskforce.authentication.UserId
import taskforce.common.LiveHttpErrorHandler
import taskforce.project.ProjectName
import taskforce.project.instances.Circe
import java.time.Instant
import taskforce.common.AppError

import taskforce.authentication.Authenticator

import sttp.tapir._
//import sttp.client3._

import taskforce.common.ResponseError
import io.circe.generic.auto._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
//import org.http4s.headers.Authorization
// import sttp.tapir.server.stub.TapirStubInterpreter
// import sttp.client3.testing.SttpBackendStub
// import sttp.tapir.integ.cats.CatsMonadError
//import sttp.client3._
// import sttp.client3.circe._
import io.circe.refined._
import org.http4s.headers.Authorization

class ProjectRoutesSuite extends HttpTestSuite with Circe {

  import arbitraries._

  implicit def decodeNewProduct: EntityDecoder[IO, ProjectName] = jsonOf
  implicit def encodeNewProduct: EntityEncoder[IO, ProjectName] = jsonEncoderOf

  val errHandler = LiveHttpErrorHandler[IO]

  def testAuthenticator(userId: UserId) = new Authenticator[IO] {
    def secureEndpoint =
      endpoint
        .securityIn(auth.bearer[String]())
        .errorOut(jsonBody[ResponseError])
        .serverSecurityLogic { _ => userId.asRight[ResponseError].pure[IO] }
  }

  val currentTime = Instant.now()

  val uri = uri"api/v1/projects"

  test("Cannot rename project to existing same name") {
    PropF.forAllF { (p1: Project, p2: Project) =>
      val projectRepo = new TestProjectRepository(List(p1, p2), currentTime) {
        override def update(id: ProjectId, newProject: ProjectName) =
          AppError.DuplicateProjectName(s"$newProject").asLeft[Project].pure[IO]
      }
      val routes =
        ProjectRoutes.make[IO](testAuthenticator(p1.author), ProjectService.make[IO](projectRepo)).routes
      PUT(p2.name, Uri.unsafeFromString(s"api/v1/projects/${p1.id.value}"),Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame"))
      ).pure[IO].flatMap { req =>
        assertHttp(routes, req)(
          Status.Conflict,
          ResponseError.DuplicateProjectName2(s"project2 name '${p2.name}' already exists")
        )
      }
    }
  }

 /* test("Cannot create project with the same name") {
    PropF.forAllF { (p1: Project, u: UserId) =>
      val projectRepo = new TestProjectRepository(List(), currentTime) {
        override def create(
            newProject: ProjectName,
            userId: UserId
        ): IO[Either[AppError.DuplicateProjectName, Project]] =
          AppError.DuplicateProjectName(s"$newProject").asLeft[Project].pure[IO]
      }

      val routes = ProjectRoutes.make[IO](testAuthenticator(u), ProjectService.make[IO](projectRepo)).routes

      POST(p1.name, uri, Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame"))).pure[IO].flatMap { req =>
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
      val routes      = ProjectRoutes.make[IO](testAuthenticator(p.author), ProjectService.make[IO](projectRepo)).routes

      GET(Uri.unsafeFromString(s"api/v1/projects/${p.id.value}")).pure[IO].flatMap { req =>
        assertHttp(routes, req)(Status.Ok, p)
      }
    }
  }

  test("cannot delete others project") {
    PropF.forAllF { (p: Project, u: UserId) =>
      val projectRepo = new TestProjectRepository(List(p), currentTime)
      val routes      = ProjectRoutes.make[IO](testAuthenticator(u), ProjectService.make[IO](projectRepo)).routes

      DELETE(Uri.unsafeFromString(s"api/v1/projects/${p.id.value}")).pure[IO].flatMap { req =>
        assertHttp(routes, req)(Status.Forbidden, ErrorMessage("BASIC-003", "User is not an owner of the resource"))
      }
    }
  }
  test("provide valid response where project is not found") {
    PropF.forAllF { (p: ProjectId, u: UserId) =>
      val projectRepo = new TestProjectRepository(List(), currentTime)
      val routes      = ProjectRoutes.make[IO](testAuthenticator(u), ProjectService.make[IO](projectRepo)).routes

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

  */
}
