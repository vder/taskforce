package taskforce.authentication

import cats.implicits._
import io.circe.parser._
import pdi.jwt.{JwtAlgorithm, JwtCirce}
import taskforce.authentication.instances.Circe
import sttp.tapir.server._
import sttp.tapir._
import cats.MonadThrow
import cats.data.EitherT
import io.circe.generic.auto._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

import sttp.model.StatusCode
import taskforce.common.ResponseError._
import taskforce.common.ResponseError



trait Authenticator[F[_]] {
  def secureEndpoint: PartialServerEndpoint[String, UserId, Unit, ResponseError, Unit, Any, F]
}

object Authenticator {

  def make[F[_]: MonadThrow](
      userRepo: UserRepository[F],
      secret: String
  ) =  new Authenticator[F] with Circe {

    def secureEndpoint: PartialServerEndpoint[String, UserId, Unit, ResponseError, Unit, Any, F] =
      endpoint
        .securityIn(auth.bearer[String]())
        .errorOut(
          oneOf[ResponseError](
            oneOfVariant[TokenDecoding](
              statusCode(StatusCode.Unauthorized)
                .and(jsonBody[TokenDecoding].description("invalid token"))
            ),
            oneOfVariant[Forbidden](
              statusCode(StatusCode.Unauthorized)
                .and(jsonBody[Forbidden].description("Unknown User"))
            ),
            oneOfVariant[NotFound](
              statusCode(StatusCode.NotFound)
                .and(jsonBody[NotFound].description("resource not found"))
            ),
            oneOfVariant[NotAuthor](
              statusCode(StatusCode.Forbidden)
                .and(jsonBody[NotAuthor].description("authorised user is not owner of the resource"))
            ),
            oneOfVariant[DuplicateProjectName2](
              
                jsonBody[DuplicateProjectName2].description("project's name is already in use").and(statusCode(StatusCode.Conflict))
            )
          )
        )
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
