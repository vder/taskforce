package taskforce

import java.util.UUID

import monix.newtypes._
import monix.newtypes.integrations.DerivedCirceCodec
import taskforce.common.DerivedDoobieMeta
import io.getquill.MappedEncoding

package object authentication {
  type UserId = UserId.Type
  object UserId
      extends NewtypeWrapped[UUID]
      with DerivedCirceCodec
      with DerivedDoobieMeta {
    implicit val encodeUserId = MappedEncoding[UserId, UUID](_.value)
    implicit val decodeUserId = MappedEncoding[UUID, UserId](UserId.apply)
  }
}
