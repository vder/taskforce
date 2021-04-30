package taskforce.repository

import cats.Monad
import cats.effect.Bracket
import cats.effect.Sync
import cats.syntax.all._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import eu.timepit.refined.types.string._
import doobie.util.transactor.Transactor
import fs2._
import taskforce.model._
import java.time.LocalDate
import java.time.LocalDateTime
import eu.timepit.refined.collection._
import eu.timepit.refined._

trait FilterRepository[F[_]] {
  def createFilter(filter: Filter): F[Filter]
  def deleteFilter(id: FilterId): F[Int]
  def getFilter(id: FilterId): F[Option[Filter]]
}

final class LiveFilterRepository[F[_]: Monad: Bracket[*[_], Throwable]](
    xa: Transactor[F]
) extends FilterRepository[F] {

  implicit val rrr: Get[List[NonEmptyString]] =
    Get[List[String]] temap (_.traverse(refineV[NonEmpty](_)))

  def createCriterias(filterId: FilterId)(criteria: Criteria) =
    criteria match {
      case In(names) =>
        sql"""insert into filters(filter_id,criteria_type,list_value)
            | values (${filterId.value},'in',${names.map(
          _.value
        )})""".stripMargin.update.run
      case Cond(field, op, value) =>
        sql"""insert into  filters(filter_id,criteria_type,field,operator,date_value)
            | values (${filterId.value},'cond',$field,${op},$value)""".stripMargin.update.run
      case State(status) =>
        sql"""insert into filters(filter_id,criteria_type,status_value)
            | values (${filterId.value},'state',${status})""".stripMargin.update.run
    }
  override def createFilter(filter: Filter): F[Filter] = {
    filter.conditions
      .traverse(createCriterias(filter.id))
      //  .map(x => { println(x); x })
      .transact(xa)
      .as(filter)
  }

  override def deleteFilter(id: FilterId): F[Int] = ???

  override def getFilter(id: FilterId): F[Option[Filter]] = {

    sql"""select criteria_type,field,operator,date_value,status_value,list_value 
        | from filters where filter_id = ${id.value}""".stripMargin
      .query[
        (
            String,
            Option[Field],
            Option[Operator],
            Option[LocalDateTime],
            Option[Status],
            Option[List[NonEmptyString]]
        )
      ]
      .to[List]
      .map(
        _.collect {
          case ("in", _, _, _, _, Some(list)) =>
            In(list)
          case ("cond", Some(field), Some(op), Some(date), _, _) =>
            Cond(field, op, date)
          case ("state", _, _, _, Some(status), _) => State(status)
        }
      )
      .transact(xa)
      .map {
        case Nil => None
        case l   => Filter(id, l).some
      }

  }
}

object LiveFilterRepository {
  def make[F[_]: Sync](xa: Transactor[F]) =
    Sync[F].delay { new LiveFilterRepository[F](xa) }
}
