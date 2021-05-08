package taskforce.repos

import cats.implicits._
import taskforce.model.User
import taskforce.model.UserId
import cats.effect.IO

case class TestUserRepository(users: List[User]) extends UserRepository[IO] {

  def getUser(userId: UserId): IO[Option[User]] = users.find(_.id == userId).pure[IO]

}
