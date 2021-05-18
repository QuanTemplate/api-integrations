package com.quantemplate.integrations.gmaps

import com.google.maps.{GeocodingApi, GeoApiContext, PendingResult}
import com.google.maps.model.GeocodingResult
import scala.concurrent.{Future, Promise}

class GeocodingService(
  context: GeoApiContext = GeoApiContext.Builder()
    .apiKey("")
    .build()
):
  def geocode(address: String): Future[Vector[GeocodingResult]] = 
    val p = Promise[Vector[GeocodingResult]]()

    GeocodingApi
      .newRequest(context)
      .address(address)
      .setCallback {
        new PendingResult.Callback:
          override def onResult(res: Array[GeocodingResult]) = p.success(res.toVector)
          override def onFailure(e: Throwable) = p.failure(e)
      }

    p.future
