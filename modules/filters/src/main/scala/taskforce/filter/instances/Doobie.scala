package taskforce.filter.instances

import cats.data.NonEmptyList
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.Get
import doobie.util.Put
import doobie.util.fragment.Fragment
import doobie.util.meta.Meta
import eu.timepit.refined._
import eu.timepit.refined.collection._
import eu.timepit.refined.types.string.NonEmptyString
import taskforce.common.Sqlizer
import taskforce.common.Sqlizer.ops._
import taskforce.filter.model._
import taskforce.common.NewTypeDoobieMeta
import _root_.cats.Show
import java.time.Instant



trait Doobie extends taskforce.task.instances.Doobie with NewTypeDoobieMeta with doobie.util.meta.LegacyInstantMetaInstance {


  implicit val showInstance: Show[Instant] =
        Show.fromToString[Instant]

  implicit val putNonEmptyList: Put[List[NonEmptyString]] =
    Put[List[String]].contramap(_.map(_.value))

  implicit val operatorMeta: Meta[Operator] =
    Meta[String].imap(helpers.operatorFromString)(helpers.operatorToString)

  implicit val statusMeta: Meta[Status] =
    Meta[String].imap(helpers.statusFromString)(helpers.statusToString)

  implicit val getNonEmptyList: Get[List[NonEmptyString]] =
    Get[List[String]] temap (_.traverse(refineV[NonEmpty](_)))

  implicit val inSqlizer: Sqlizer[Criteria.In] = (in: Criteria.In) => NonEmptyList
    .fromList(in.names)
    .map(x => Fragments.in(fr"p.name", x))
    .getOrElse(Fragment.empty)

  implicit val stateSqlizer: Sqlizer[Criteria.State] = new Sqlizer[Criteria.State] {
    def toFragment(s: Criteria.State) =
      s.status match {
        case Status.All      => fr"1=1"
        case Status.Inactive => fr"""t.deleted is not null"""
        case Status.Active   => fr"""t.deleted is null"""
      }
  }

  implicit val operatorSqlizer: Sqlizer[Operator] = new Sqlizer[Operator] {
    def toFragment(o: Operator) =
      Fragment.const(o match {
        case Operator.Eq   => "="
        case Operator.Gt   => ">"
        case Operator.Gteq => ">="
        case Operator.Lt   => "<"
        case Operator.Lteq => "<="
      })
  }

  implicit val taskCreatedDateSqlizer: Sqlizer[Criteria.TaskCreatedDate] =
    new Sqlizer[Criteria.TaskCreatedDate] {
      def toFragment(t: Criteria.TaskCreatedDate) =
        Fragment.const(s" t.started ") ++ t.op.toFragment ++ fr"${t.date}"
    }

  implicit lazy val criteriaSqlizer: Sqlizer[Criteria] =
    new Sqlizer[Criteria] {
      def toFragment(t: Criteria) =
        t match {
          case x: Criteria.TaskCreatedDate => x.toFragment
          case x: Criteria.In              => x.toFragment
          case x: Criteria.State           => x.toFragment
        }
    }

  implicit lazy val pageSqlizer: Sqlizer[Page] =
    new Sqlizer[Page] {
      def toFragment(p: Page) =
        p match {
          case Page(PageNo(pageNo), PageSize(pageSize)) =>
            Fragment.const(
              s" limit ${pageSize.value} offset ${(pageNo.value - 1) * pageSize.value}"
            )
        }
    }
  implicit lazy val sortBySqlizer: Sqlizer[SortBy] =
    new Sqlizer[SortBy] {
      def toFragment(s: SortBy) =
        Fragment.const(s.field match {
          case Field.CreatedDate => s"order by t.started ${s.order}"
          case Field.UpdatedDate =>
            s"order by coalesce(t.started,p.created) ${s.order}"
        })
    }
}
