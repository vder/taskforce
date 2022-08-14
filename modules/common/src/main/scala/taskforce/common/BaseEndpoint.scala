package taskforce.common

import sttp.tapir.{endpoint => tapirEndpoint, _}
import sttp.tapir.json.circe._
import sttp.model._
import io.circe.generic.auto._
import sttp.tapir.generic.auto._

trait BaseEndpoint extends instances.Circe {
  val endpoint: Endpoint[Unit, Unit, ResponseError, Unit, Any] =
    tapirEndpoint
      .in("api")
      .in("v1")
      .errorOut(
        oneOf[ResponseError](
          oneOfVariant[ResponseError.TokenDecoding](
            statusCode(StatusCode.Unauthorized)
              .description("Unauthorized")
              .and(jsonBody[ResponseError.TokenDecoding].description("Cannot decode given authorization token"))
          ),
          oneOfVariant[ResponseError.Forbidden](
            statusCode(StatusCode.Unauthorized)
              .description("Unauthorized")
              .and(
                jsonBody[ResponseError.Forbidden]
                  .description("Unknown User")
                  .description("User given in authorization token does not exist")
              )
          ),
          oneOfVariant[ResponseError.NotFound](
            statusCode(StatusCode.NotFound)
              .description("NotFound")
              .and(
                jsonBody[ResponseError.NotFound]
                  .description("Resource not found")
                  .description("Given resource does not exists")
              )
          ),
          oneOfVariant[ResponseError.NotAuthor](
            statusCode(StatusCode.Forbidden)
              .description("Forbidden")
              .and(
                jsonBody[ResponseError.NotAuthor]
                  .description("Authorized user is not owner of the resource and cannot make changes")
              )
          ),
          oneOfVariant[ResponseError.DuplicateProjectName](
            jsonBody[ResponseError.DuplicateProjectName]
              .description("Project's name is already in use")
              .and(statusCode(StatusCode.Conflict).description("Conflict"))
          ),
          oneOfVariant[ResponseError.WrongPeriodError](
            jsonBody[ResponseError.WrongPeriodError]
              .description("User alredy reported given time")
              .and(statusCode(StatusCode.Conflict).description("Conflict"))
          ),
          oneOfVariant[ResponseError.BadRequest](
            jsonBody[ResponseError.BadRequest]
              .description("User alredy reported given time")
              .and(statusCode(StatusCode.BadRequest))
          )
        )
      )

}
