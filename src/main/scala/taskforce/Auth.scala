package taskforce

import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt._
import io.circe.parser._
import pdi.jwt._
import taskforce.model._
import taskforce.repos.UserRepository

trait Auth[F[_]] {
  def authenticate: JwtToken => JwtClaim => F[Option[UserId]]
  val jwtAuth: JwtSymmetricAuth
}

class LiveAuth[F[_]: Sync](userRepo: UserRepository[F], secret: String) extends Auth[F] {
  def authenticate: JwtToken => (JwtClaim => F[Option[UserId]]) =
    _ =>
      c => {
        val userEither = for {
          json <- parse(c.content)
          user <- json.as[User]
        } yield user

        for {
          userJwt <- Sync[F].fromEither(userEither)
          userOpt <- userRepo.getUser(userJwt.id)
          userIdOpt = userOpt.map(_.id)
        } yield userIdOpt
      }

  override val jwtAuth: JwtSymmetricAuth =
    JwtAuth.hmac(secret, JwtAlgorithm.HS256)
}

object LiveAuth {
  def make[F[_]: Sync](userRepo: UserRepository[F], secret: String) =
    Sync[F].delay(new LiveAuth(userRepo, secret))
}
