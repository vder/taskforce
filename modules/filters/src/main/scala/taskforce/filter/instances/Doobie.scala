package taskforce.filter.instances

import _root_.cats.Show
import cats.data.NonEmptyList
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.refined.implicits.*
import doobie.util.fragment.Fragment
import doobie.util.meta.Meta
import doobie.util.{Get, Put}
import eu.timepit.refined.*
import eu.timepit.refined.collection.*
import eu.timepit.refined.types.string.NonEmptyString
import taskforce.common.{NewTypeDoobieMeta, Sqlizer}
import taskforce.filter.model.*

import java.time.Instant

trait Doobie
    extends taskforce.task.instances.Doobie
    with NewTypeDoobieMeta
    with doobie.util.meta.LegacyInstantMetaInstance {

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

  given Sqlizer[Criteria.In] with
    extension (in: Criteria.In)
      def toFragment: Fragment =
        NonEmptyList.fromList(in.names).map(x => Fragments.in(fr"p.name", x)).getOrElse(Fragment.empty)

  given Sqlizer[Criteria.State] with
    extension (s: Criteria.State)
      def toFragment: Fragment =
        s.status match {
          case Status.All      => fr"1=1"
          case Status.Inactive => fr"""t.deleted is not null"""
          case Status.Active   => fr"""t.deleted is null"""
        }

  given Sqlizer[Operator] with
    extension (o: Operator)
      def toFragment =
        Fragment.const(o match {
          case Operator.Eq   => "="
          case Operator.Gt   => ">"
          case Operator.Gteq => ">="
          case Operator.Lt   => "<"
          case Operator.Lteq => "<="
        })

  given Sqlizer[Criteria.TaskCreatedDate] with
    extension (t: Criteria.TaskCreatedDate)
      def toFragment =
        Fragment.const(s" t.started ") ++ t.op.toFragment ++ fr"${t.date}"

  given Sqlizer[Criteria] with
    extension (t: Criteria)
      def toFragment =
        t match {
          case x: Criteria.TaskCreatedDate => summon[Sqlizer[Criteria.TaskCreatedDate]].toFragment(x)
          case x: Criteria.In              => summon[Sqlizer[Criteria.In]].toFragment(x)
          case x: Criteria.State           => summon[Sqlizer[Criteria.State]].toFragment(x)
        }

  given Sqlizer[Page] with
    extension (p: Page)
      def toFragment =
        p match {
          case Page(PageNo(pageNo), PageSize(pageSize)) =>
            Fragment.const(
              s" limit ${pageSize.value} offset ${(pageNo.value - 1) * pageSize.value}"
            )
        }

  given Sqlizer[SortBy] with
    extension (s: SortBy)
      def toFragment =
        Fragment.const(s.field match {
          case Field.CreatedDate => s"order by t.started ${s.order}"
          case Field.UpdatedDate =>
            s"order by coalesce(t.started,p.created) ${s.order}"
        })

}
