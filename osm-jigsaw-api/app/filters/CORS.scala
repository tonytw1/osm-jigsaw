package filters

import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CORS {
  
  val AllowCORSHeaders = Seq(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST",
      "Access-Control-Expose-Headers" -> "X-Total-Count"
  )

  def withCORS(request: Request[AnyContent], result: Future[Result]): Future[Result] = {
    result.map { r =>
      val requestedHeaders = request.headers.get("Access-Control-Request-Headers")

      val withAllowedHeaders = requestedHeaders.fold {
        AllowCORSHeaders
      }{ rh =>
        AllowCORSHeaders :+ ("Access-Control-Allow-Headers" -> rh)  // TODO filter allowed headers - lowercase as per RFC
      }

      r.withHeaders(withAllowedHeaders: _*)
    }
  }

}
