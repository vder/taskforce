package taskforce.authentication

import cats.implicits._
import cats.effect.IO

case class TestUserRepository(users: List[User]) extends UserRepository[IO] {

  override def create(user: User): IO[Int] = 1.pure[IO]

  def find(userId: UserId): IO[Option[User]] =
    users.find(_.id == userId).pure[IO]

}
