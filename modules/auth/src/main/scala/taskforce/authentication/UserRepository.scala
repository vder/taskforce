package taskforce.authentication

import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import taskforce.common.NewTypeDoobieMeta
import cats.effect.kernel.MonadCancelThrow

trait UserRepository[F[_]] {
  def find(userId: UserId): F[Option[User]]
  def create(user: User): F[Int]
}

object UserRepository {
  def make[F[_]: MonadCancelThrow](xa: Transactor[F]) =
    new UserRepository[F] with NewTypeDoobieMeta {

      override def create(user: User): F[Int] =
        sql.insert(user).update.run.transact(xa)

      override def find(userId: UserId): F[Option[User]] =
        sql
          .find(userId)
          .query[User]
          .option
          .transact(xa)

      private object sql {
        def insert(user: User) = sql"insert into users(id) values(${user.id})"
        def find(userId: UserId) =
          sql"select id from users where id =${userId.value}"
      }

    }
}
