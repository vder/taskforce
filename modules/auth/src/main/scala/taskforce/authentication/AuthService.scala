package taskforce.authentication

import cats.implicits._
import io.circe.parser._
import pdi.jwt.{JwtAlgorithm, JwtCirce}
import taskforce.authentication.instances.Circe

import cats.data.EitherT

import taskforce.common.ResponseError._
import taskforce.common.ResponseError
import cats.Monad



trait AuthService[F[_]] {

  def authenticate(bearerToken: String): F[Either[ResponseError, UserId]]

}

object AuthService {

  def apply[F[_] : Monad](userRepository: UserRepository[F], secret: String): AuthService[F] =
    new AuthService[F] with Circe {
      def authenticate(bearerToken: String): F[Either[ResponseError, UserId]] = {
        (for {
          jwtClaim <- EitherT.fromEither[F](
            JwtCirce
              .decode(bearerToken, secret, Seq(JwtAlgorithm.HS256))
              .toEither
              .leftMap[ResponseError](_ => TokenDecoding(s"Failed to decode a token: $bearerToken"))
          )
          userFromToken <- EitherT.fromEither[F](
            parse(jwtClaim.content)
              .flatMap(_.as[User])
              .leftMap[ResponseError](_ => Forbidden("Invalid token"))
          )
          validatedUser <- EitherT(
            userRepository.find(userFromToken.id)
              .map(_.toRight[ResponseError](Forbidden("Invalid token")))
          )
        } yield validatedUser.id).value
      }
    }

}