package taskforce.authentication

import cats.effect.Sync
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import cats.effect.kernel.MonadCancel

trait UserRepository[F[_]] {
  def find(userId: UserId): F[Option[User]]
  def create(user: User): F[Int]
}

final class LiveUserRepository[F[_]:  MonadCancel[*[_], Throwable]](
    xa: Transactor[F]
) extends UserRepository[F] {

  override def create(user: User): F[Int] =
    sql.insert(user).update.run.transact(xa)

  override def find(userId: UserId): F[Option[User]] =
    sql
      .find(userId)
      .query[User]
      .option
      .transact(xa)

  object sql {
    def insert(user: User)   = sql"insert into users(id) values(${user.id})"
    def find(userId: UserId) = sql"select id from users where id =${userId.value}"
  }

}

object LiveUserRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveUserRepository[F](xa) }
}
