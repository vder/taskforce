import java.util.UUID
import io.circe.Json
import java.time.LocalDateTime
import taskforce.model._
import eu.timepit.refined._
import eu.timepit.refined.collection._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.refined._

val f = Filter(
  FilterId(UUID.randomUUID()),
  List(
    In(List(refineMV[NonEmpty]("aaaa"), refineMV("bbbb"))),
    Cond(From, Lt, LocalDateTime.now()),
    State(Active)
  )
)

f.asJson

val s = f.asJson.noSpaces

val z = parse(s).getOrElse(Json.fromInt(1))

println("ok1")

val t = Cond(From, Lt, LocalDateTime.now()).asJson

val cond = t.as[Cond]

println(t.noSpaces)

val zz = (From: Field).asJson.as[Field]

println(zz)

State(Active).asJson.as[State]

In(List(refineMV[NonEmpty]("aaaa"), refineMV("bbbb"))).asJson.as[In]

Cond(From, Lt, LocalDateTime.now()).asJson.as[Cond]

f.asJson.as[Filter]

(Map("key1" -> "value1"), Map("key2" -> "value2")).asJson.noSpaces
