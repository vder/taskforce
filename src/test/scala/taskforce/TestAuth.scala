package taskforce

import dev.profunktor.auth.jwt._
import io.circe.parser._
import pdi.jwt._
import taskforce.model._
import cats.effect.IO

final case class TestAuth(users: List[User]) extends Auth[IO] {

  override val jwtAuth: JwtSymmetricAuth =
    JwtAuth.hmac("53cr3t", JwtAlgorithm.HS256)

  override def authenticate: JwtToken => JwtClaim => IO[Option[UserId]] =
    t =>
      c => {
        val userEither = for {
          json <- parse(c.content)
          user <- json.as[User]
        } yield user

        for {
          userJwt <- IO.fromEither(userEither)
        } yield users.collectFirst { case u: User if u.id == userJwt.id => u.id }
      }

}
