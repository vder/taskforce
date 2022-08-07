package taskforce.common

import sttp.tapir.{endpoint => tapirEndpoint, _}
import sttp.tapir.json.circe._
import sttp.model._
import io.circe.generic.auto._
import sttp.tapir.generic.auto._

object BaseApi extends instances.Circe {
  val endpoint: Endpoint[Unit, Unit, ResponseError, Unit, Any] =
    tapirEndpoint
      .errorOut(
        oneOf[ResponseError](
          oneOfVariant[ResponseError.TokenDecoding](
            statusCode(StatusCode.Unauthorized)
              .and(jsonBody[ResponseError.TokenDecoding].description("invalid token"))
          ),
          oneOfVariant[ResponseError.Forbidden](
            statusCode(StatusCode.Unauthorized)
              .and(jsonBody[ResponseError.Forbidden].description("Unknown User"))
          ),
          oneOfVariant[ResponseError.NotFound](
            statusCode(StatusCode.NotFound)
              .and(jsonBody[ResponseError.NotFound].description("resource not found"))
          ),
          oneOfVariant[ResponseError.NotAuthor](
            statusCode(StatusCode.Forbidden)
              .and(jsonBody[ResponseError.NotAuthor].description("authorised user is not owner of the resource"))
          ),
          oneOfVariant[ResponseError.DuplicateProjectName2](
            jsonBody[ResponseError.DuplicateProjectName2]
              .description("project's name is already in use")
              .and(statusCode(StatusCode.Conflict))
          ),
          oneOfVariant[ResponseError.WrongPeriodError](
            jsonBody[ResponseError.WrongPeriodError]
              .description("User alredy reported given time")
              .and(statusCode(StatusCode.Conflict))
          )
        )
      )

}
