package taskforce.authentication

import cats.implicits._
import io.circe.parser._
import pdi.jwt.{JwtAlgorithm, JwtCirce}
import taskforce.authentication.instances.Circe
import sttp.tapir.server._
import sttp.tapir._
import cats.MonadThrow
import cats.data.EitherT

final class TaskForceAuthenticator[F[_]: MonadThrow](
    userRepo: UserRepository[F],
    secret: String
) extends Circe {

  def secureEndpoint: PartialServerEndpoint[String, UserId, Unit, String, Unit, Any, F] = {

    endpoint.securityIn(auth.bearer[String]()).errorOut(plainBody[String]).serverSecurityLogic { encodedString =>
      {
        val userEither =
          for {
            jwtClaim <-
              JwtCirce
                .decode(encodedString, secret, Seq(JwtAlgorithm.HS256))
                .toEither
                .leftMap(_ => s"token decode error: $encodedString")
            user <- parse(jwtClaim.content)
              .flatMap(_.as[User])
              .leftMap(_ => s"cannot parse user: ${jwtClaim.content}")
          } yield user

        val userId = for {
          user <- EitherT.fromEither[F](userEither)
          validatedUserId <- EitherT(
            userRepo.find(user.id).map(_.toRight("unAuthorized"))
          )
        } yield validatedUserId.id

        userId.value
      }
    }
  }
}
