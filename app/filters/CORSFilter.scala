package filters

import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class CORSFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter with CORS {

  def apply(nextFilter: RequestHeader => Future[Result]) (requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      result.withHeaders(AllowCORSHeaders: _*)
    }

  }

}