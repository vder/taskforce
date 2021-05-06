package taskforce.repository

import cats.Monad
import cats.effect.{Bracket, Sync}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import taskforce.model._

trait UserRepository[F[_]] {
  def getUser(userId: UserId): F[Option[User]]

}

final class LiveUserRepository[F[_]: Monad: Bracket[*[_], Throwable]](
    xa: Transactor[F]
) extends UserRepository[F] {

  override def getUser(userId: UserId): F[Option[User]] =
    sql"select id from users where id =${userId.value}"
      .query[User]
      .option
      .transact(xa)

}

object LiveUserRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveUserRepository[F](xa) }
}
