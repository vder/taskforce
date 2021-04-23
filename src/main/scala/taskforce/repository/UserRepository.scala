package taskforce.repository

import java.util.UUID
import taskforce.model.domain._
import doobie.util.transactor.Transactor
import doobie.implicits._
import cats.Monad
import cats.syntax.all._

import cats.effect.Bracket
import doobie.util.meta.Meta
import cats.effect.Sync
import doobie.postgres._
import doobie.postgres.implicits._

trait UserRepository[F[_]] {
  def validateUser(userId: UserId): F[Option[UserId]]
}

final class LiveUserRepository[F[_]: Monad: Bracket[*[_], Throwable]](
    xa: Transactor[F]
) extends UserRepository[F] {

  override def validateUser(userId: UserId): F[Option[UserId]] =
    sql"select id from users where id =${userId.id}"
      .query[UUID]
      .option
      .transact(xa)
      .map(_.map(UserId.apply))

}

object LiveUserRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveUserRepository[F](xa) }
}
