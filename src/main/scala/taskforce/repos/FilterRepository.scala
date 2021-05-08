package taskforce.repos

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
import java.time.LocalDateTime
import eu.timepit.refined.collection._
import eu.timepit.refined._
import cats.data.NonEmptyList

trait FilterRepository[F[_]] {
  def createFilter(filter: Filter): F[Filter]
  def deleteFilter(id: FilterId): F[Int]
  def getFilter(id: FilterId): F[Option[Filter]]
  def getRows(filter: Filter): Stream[F, (Project, Option[Task])]
  def getAllFilters: Stream[F, Filter]
}

final class LiveFilterRepository[F[_]: Monad: Bracket[*[_], Throwable]](
    xa: Transactor[F]
) extends FilterRepository[F] {

  override def getAllFilters: Stream[F, Filter] = ???

  implicit val getNonEmptyList: Get[List[NonEmptyString]] =
    Get[List[String]] temap (_.traverse(refineV[NonEmpty](_)))

  implicit val purNonEmptyList: Put[List[NonEmptyString]] =
    Put[List[String]].contramap(_.map(_.value))

  def toSql(c: Criteria): Fragment =
    c match {
      case In(list) =>
        NonEmptyList
          .fromList(list)
          .map(x => Fragments.in(fr"p.name", x))
          .getOrElse(fr"")
      case State(All)                => fr"1=1"
      case State(Deactive)           => fr"""t.deleted is not null"""
      case State(Active)             => fr"""t.deleted is null"""
      case TaskCreatedDate(op, date) => fr""" t.started ${op.toSql}  ${date}""""
    }

  override def getRows(filter: Filter): Stream[F, (Project, Option[Task])] = {
    val select = fr"""select p.id,
                       |     p.name,
                       |     p.author,
                       |     p.created,
                       |     p.deleted,
                       |     t.id,
                       |     t.project_id,
                       |     t.author,
                       |     t.started,
                       |     t.duration,
                       |     t.volume,
                       |     t.deleted,
                       |     t.comment 
                       |from projects p 
                       |left join tasks t
                       |  on p.id = t.project_id""".stripMargin

    val where = filter.conditions.foldLeft(fr" where 1=1 ")((fr, criteria) => fr ++ fr" And " ++ toSql(criteria))

    println(select ++ where)
    (select ++ where).query[(Project, Option[Task])].stream.transact(xa)
  }

  def createCriterias(filterId: FilterId)(criteria: Criteria) =
    criteria match {
      case In(names) =>
        sql"""insert into filters(filter_id,criteria_type,list_value)
            | values (${filterId.value},'in',${names.map(
          _.value
        )})""".stripMargin.update.run
      case TaskCreatedDate(op, value) =>
        sql"""insert into filters(filter_id,criteria_type,operator,date_value)
                 | values (${filterId.value},
                 |         'cond',
                 |         ${op},
                 |         $value)""".stripMargin.update.run
      case State(status) =>
        sql"""insert into filters(filter_id,criteria_type,status_value)
                 | values (${filterId.value},
                 |         'state',
                 |         ${status})""".stripMargin.update.run
    }
  override def createFilter(filter: Filter): F[Filter] = {
    filter.conditions
      .traverse(createCriterias(filter.id))
      .transact(xa)
      .as(filter)
  }

  override def deleteFilter(id: FilterId): F[Int] =
    sql"delete from filters where filter_id = $id".update.run.transact(xa)

  override def getFilter(id: FilterId): F[Option[Filter]] = {

    sql"""select criteria_type,operator,date_value,status_value,list_value 
         |  from filters 
         | where filter_id = ${id.value}""".stripMargin
      .query[
        (
            String,
            Option[Operator],
            Option[LocalDateTime],
            Option[Status],
            Option[List[NonEmptyString]]
        )
      ]
      .to[List]
      .map(
        _.collect {
          case ("in", _, _, _, Some(list)) =>
            In(list)
          case ("cond", Some(op), Some(date), _, _) =>
            TaskCreatedDate(op, date)
          case ("state", _, _, Some(status), _) => State(status)
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
