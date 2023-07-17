
import javax.inject.Inject

import filters.CORSFilter
import filters.RequestTime
import play.api.http.HttpFilters

class Filters @Inject()(CORSFilter: CORSFilter, requestTime: RequestTime) extends HttpFilters {
  val filters = Seq(CORSFilter, requestTime)
}
