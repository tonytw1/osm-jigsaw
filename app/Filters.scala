
import javax.inject.Inject

import filters.CORSFilter
import play.api.http.HttpFilters

class Filters @Inject()(CORSFilter: CORSFilter) extends HttpFilters {
  val filters = Seq(CORSFilter)
}
