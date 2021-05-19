package com.quantemplate.integrations.gmaps

import com.google.maps.{GeocodingApi, GeoApiContext, PendingResult}
import com.google.maps.model.GeocodingResult
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext
import scala.util.Using.*
import cats.syntax.traverse.given

import com.quantemplate.integrations.common.Config

class GeocodingService()(using conf: Config.GoogleMaps):
  private lazy val context = GeoApiContext.Builder()
    .apiKey(conf.apiKey)
    .build()

  def shutdown() = context.shutdown()

  def geocode(address: Vector[String])(using ExecutionContext): Future[Vector[Vector[GeocodingResult]]] = 
    address.map(geocode).sequence

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

object GeocodingService:
  given Releasable[GeocodingService] with 
    def release(r: GeocodingService) = r.shutdown()
