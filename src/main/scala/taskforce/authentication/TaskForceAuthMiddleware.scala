package taskforce.authentication

import cats.MonadError
import cats.data.{EitherT, Kleisli, OptionT}
import cats.implicits._
import io.circe.parser._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthScheme, AuthedRoutes, Credentials, Request}
import pdi.jwt.{JwtAlgorithm, JwtCirce}

object TaskForceAuthMiddleware extends instances.Circe {

  def apply[F[_]: MonadError[*[_], Throwable]](
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
                .collect {
                  case Authorization(Credentials.Token(AuthScheme.Bearer, t)) => t
                }
                .toRight("missing Authorization header")
            jwtClaim <-
              JwtCirce
                .decode(encodedString, secret, Seq(JwtAlgorithm.HS256))
                .toEither
                .leftMap(_ => "token decode error")
            user <- parse(jwtClaim.content)
              .flatMap(_.as[User])
              .leftMap(_ => "cannot parse user")
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
