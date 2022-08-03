package taskforce.authentication

import cats.implicits._
import io.circe.parser._
import pdi.jwt.{JwtAlgorithm, JwtCirce}
import taskforce.authentication.instances.Circe
import sttp.tapir.server._
import sttp.tapir._

import cats.data.EitherT

import taskforce.common.ResponseError._
import taskforce.common.ResponseError
import cats.MonadThrow

trait Authenticator[F[_]] {
  
    def secureEndpoints[SECURITY_INPUT, INPUT, OUTPUT](
        endpoints: Endpoint[String, INPUT, ResponseError, OUTPUT, Any]
    ): PartialServerEndpoint[String, UserId, INPUT, ResponseError, OUTPUT, Any, F]
}

object Authenticator {

  def make[F[_]: MonadThrow](
      userRepo: UserRepository[F],
      secret: String
  ) = new Authenticator[F] with Circe {

    def secureEndpoints[SECURITY_INPUT, INPUT, OUTPUT](
        endpoints: Endpoint[String, INPUT, ResponseError, OUTPUT, Any]
    ): PartialServerEndpoint[String, UserId, INPUT, ResponseError, OUTPUT, Any, F] =
    endpoints
      .serverSecurityLogic { encodedString =>
        val userId = for {
          jwtClaim <- EitherT.fromEither[F](
            JwtCirce
              .decode(encodedString, secret, Seq(JwtAlgorithm.HS256))
              .toEither
              .leftMap[ResponseError](_ => TokenDecoding(s"invalid token: $encodedString"))
          )
          user <- EitherT.fromEither[F](
            parse(jwtClaim.content)
              .flatMap(_.as[User])
              .leftMap[ResponseError](_ => Forbidden(s"invalid token: $encodedString"))
          )
          validatedUserId <- EitherT(
            userRepo.find(user.id).map(_.toRight[ResponseError](Forbidden(s"invalid token: $encodedString")))
          )
        } yield validatedUserId.id

        userId.value

      }

  }
}
