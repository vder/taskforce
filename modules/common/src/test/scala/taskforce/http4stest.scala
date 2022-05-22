package taskforce

import cats.effect.IO
import io.circe._
import io.circe.syntax._
import munit.CatsEffectSuite
import munit.ScalaCheckEffectSuite
import org.http4s._
import org.http4s.circe._

trait HttpTestSuite extends CatsEffectSuite with ScalaCheckEffectSuite {

  def assertHttp[A: Encoder](routes: HttpRoutes[IO], req: Request[IO])(
      expectedStatus: Status,
      expectedBody: A
  ) =
    routes.run(req).value.flatMap {
      case Some(resp) =>
        resp.asJson.map { json =>
          assertEquals((resp.status, json.dropNullValues), (expectedStatus, expectedBody.asJson.dropNullValues))
        }
      case None => fail("route not found")
    }

  def assertHttpStatus(routes: HttpRoutes[IO], req: Request[IO])(expectedStatus: Status) =
    routes.run(req).value.map {
      case Some(resp) =>
        assertEquals(resp.status, expectedStatus)
      case None => fail("route nout found")
    }

  def assertHttpFailure(routes: HttpRoutes[IO], req: Request[IO])(expected: Throwable) =
    routes.run(req).value.attempt.map { x =>
      {
        x match {
          case Left(err) =>
            assertEquals(err, expected)
          case Right(_) => fail("expected a failure")
        }
      }
    }

}
