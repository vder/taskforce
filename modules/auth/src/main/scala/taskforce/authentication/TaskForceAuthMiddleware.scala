package taskforce.authentication

import cats.data.{EitherT, Kleisli, OptionT}
import cats.implicits._
import io.circe.parser._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthScheme, AuthedRoutes, Credentials, Request}
import pdi.jwt.{JwtAlgorithm, JwtCirce}
import cats.MonadThrow
import taskforce.authentication.instances.Circe

object TaskForceAuthMiddleware extends Circe {

  def apply[F[_]: MonadThrow](
      userRepo: UserRepository[F],
      secret: String
  ): AuthMiddleware[F, UserId] = {
    val dsl = new Http4sDsl[F] {}; import dsl._

    def authUser(
        userRepo: UserRepository[F],
        secret: String
    ): Kleisli[F, Request[F], Either[String, UserId]] =
      Kleisli { request =>
        val userEither =
          for {
            encodedString <-
              request.headers
                .get[Authorization]
                .fold(s"missing Authorization header: ${request}".asLeft[String]) {
                  case Authorization(Credentials.Token(AuthScheme.Bearer, t)) =>
                    t.asRight[String]
                  case header => s"invalid header type $header".asLeft[String]
                }
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

    val onFailure: AuthedRoutes[String, F] =
      Kleisli(req => OptionT.liftF(Forbidden(req.context)))

    AuthMiddleware(authUser(userRepo, secret), onFailure)
  }
}
