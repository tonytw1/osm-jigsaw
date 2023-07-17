package areas

import com.esri.core.geometry.Polygon
import com.google.common.cache.CacheBuilder
import play.api.Configuration

import javax.inject.Inject

class PolygonCache  @Inject()(configuration: Configuration) {

  private val cache = CacheBuilder.newBuilder()
    .maximumSize(configuration.get[Int]("polygonCache.size"))
    .build[java.lang.Long, Polygon]

  def getIfPresent(key: Long): Polygon = cache.getIfPresent(key)
  def put(key: Long, value: Polygon): Unit = cache.put(key, value)

}
