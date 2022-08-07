package taskforce.common

import io.circe.Encoder
import fs2.Chunk

trait StreamingResponse {

  def wrapInArray[F[_], A](implicit enc: Encoder[A]): fs2.Pipe[F, A, Byte] = stream =>
    (fs2.Stream.emit("[") ++ stream.map(enc(_).noSpaces).intersperse(",") ++ fs2.Stream.emit("]"))
      .map(_.getBytes("UTF-8"))
      .flatMap(a => fs2.Stream.chunk(Chunk.array(a)))

}
