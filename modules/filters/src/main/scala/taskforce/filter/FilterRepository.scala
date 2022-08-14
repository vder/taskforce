package taskforce.filter

import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.fragments
import doobie.util.transactor.Transactor
import eu.timepit.refined.types.string._
import fs2.Stream
import java.util.UUID
import taskforce.common.Sqlizer.ops._
import taskforce.project.Project
import taskforce.task.Task
import cats.effect.kernel.MonadCancelThrow
import eu.timepit.refined.cats._
import org.typelevel.log4cats.Logger
import java.time.Instant
import taskforce.filter.model._

trait FilterRepository[F[_]] {
  def create(filter: Filter): F[Unit]
  def delete(id: FilterId): F[Int]
  def find(id: FilterId): F[Option[Filter]]
  def execute(
      filter: Filter,
      sortByOption: Option[SortBy],
      page: Page
  ): Stream[F, FilterResultRow]
  def list: Stream[F, Filter]
}

object FilterRepository {
  def make[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): FilterRepository[F] = new FilterRepository[F]
    with instances.Doobie {

    type CriteriaRow = (
        String,
        Option[Operator],
        Option[Instant],
        Option[Status],
        Option[List[NonEmptyString]]
    )

    private val dbRow2Condition: PartialFunction[
      CriteriaRow,
      Criteria
    ] = {
      case ("in", _, _, _, Some(list)) =>
        Criteria.In(list)
      case ("cond", Some(op), Some(date), _, _) =>
        Criteria.TaskCreatedDate(op, date)
      case ("state", _, _, Some(status), _) => Criteria.State(status)
    }

    override def list: Stream[F, Filter] =
      sql.getAll
        .query[
          (
              UUID,
              CriteriaRow
          )
        ]
        .stream
        .transact(xa)
        .map { case (k, v) => (k, dbRow2Condition(v)) }
        .groupAdjacentBy { case (uuid, _) => uuid }
        .map { case (uuid, criteriaChunk) =>
          Filter(FilterId(uuid), criteriaChunk.map(_._2).toList)
        }

    override def execute(
        filter: Filter,
        sortByOption: Option[SortBy],
        page: Page
    ): Stream[F, FilterResultRow] = {

      val whereClause =
        fragments.whereAnd(filter.conditions.map(_.toFragment): _*)
      val orderClause = sortByOption.fold(Fragment.empty)(_.toFragment)
      val limitClause = page.toFragment
      val sqlQuery    = sql.getData ++ whereClause ++ orderClause ++ limitClause

      Stream.eval[F, Unit](Logger[F].debug(s"test: $sqlQuery")) >>
        sqlQuery
          .query[(Project, Option[Task])]
          .stream
          .transact(xa)
          .map(x => FilterResultRow.fromTuple(x))
    }

    def createCriterias(filterId: FilterId)(criteria: Criteria): doobie.ConnectionIO[Int] =
      criteria match {
        case in @ Criteria.In(_) =>
          sql.createInCriteria(filterId, in).update.run
        case date @ Criteria.TaskCreatedDate(_, _) =>
          sql.createDateCriteria(filterId, date).update.run
        case state @ Criteria.State(_) =>
          sql.createStateCriteria(filterId, state).update.run
      }
    override def create(filter: Filter): F[Unit] = {
      filter.conditions
        .traverse(createCriterias(filter.id))
        .transact(xa)
        .void
    }

    override def delete(id: FilterId): F[Int] =
      sql.delete(id).update.run.transact(xa)

    override def find(id: FilterId): F[Option[Filter]] =
      sql
        .get(id)
        .query[CriteriaRow]
        .to[List]
        .map(
          _.collect { dbRow2Condition }
        )
        .transact(xa)
        .map {
          case Nil => None
          case l   => Filter(id, l).some
        }

    private object sql {

      val getData = fr"""select p.id,
                      |      p.name,
                      |      p.author,
                      |      p.created,
                      |      p.deleted,
                      |      t.id,
                      |      t.project_id,
                      |      t.author,
                      |      t.started,
                      |      t.duration,
                      |      t.volume,
                      |      t.deleted,
                      |      t.comment
                      | from projects p left join tasks t 
                      |   on t.project_id = p.id""".stripMargin

      val getAll = sql"""select filter_id,
                      |       criteria_type,
                      |       operator,
                      |       date_value,
                      |       status_value,
                      |       list_value 
                      |  from filters order by filter_id""".stripMargin

      def createStateCriteria(id: FilterId, state: Criteria.State) =
        sql"""insert into filters(filter_id,criteria_type,status_value)
           | values (${id},
           |         'state',
           |         ${state.status})""".stripMargin

      def createInCriteria(id: FilterId, in: Criteria.In) =
        sql"""insert into filters(filter_id,criteria_type,list_value)
          | values (${id},
          |         'in',
          |         ${in.names.map(_.value)}
          |         )""".stripMargin

      def createDateCriteria(id: FilterId, date: Criteria.TaskCreatedDate) =
        sql"""insert into filters(filter_id,criteria_type,operator,date_value)
               | values (${id},
               |         'cond',
               |         ${date.op},
               |         ${date.date})""".stripMargin
      def delete(id: FilterId) = sql"delete from filters where filter_id = $id"
      def get(id: FilterId) =
        sql"""select criteria_type,operator,date_value,status_value,list_value 
          |  from filters 
          | where filter_id = ${id}""".stripMargin
    }
  }
}
