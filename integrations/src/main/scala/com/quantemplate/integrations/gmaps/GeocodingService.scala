package com.quantemplate.integrations.gmaps

import com.google.maps.{GeocodingApi, GeoApiContext, PendingResult}
import com.google.maps.errors.ZeroResultsException
import com.google.maps.model.GeocodingResult
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext
import com.google.maps.model.AddressComponent
import com.google.maps.model.AddressComponentType
import scala.util.Using.*
import cats.syntax.traverse.given
import cats.syntax.option.given

import com.quantemplate.integrations.common.{Config, View, Xlsx}

class GeocodingService()(using conf: Config.GoogleMaps):
  private lazy val context = GeoApiContext.Builder()
    .apiKey(conf.apiKey)
    .build()

  def shutdown() = context.shutdown()

  def getGeocodedRows(
    addresses: Vector[String], 
    columns: Vector[AddressComponentType]
  )(using ExecutionContext) = 
    addresses
      .map(geocode)
      .sequence
      .map(constructRows(columns))
   
  def geocode(address: String): Future[Option[Vector[GeocodingResult]]] = 
    val p = Promise[Option[Vector[GeocodingResult]]]()

    GeocodingApi
      .newRequest(context)
      .address(address)
      .setCallback {
        new PendingResult.Callback:
          override def onResult(res: Array[GeocodingResult]) =
            p.success(Option.unless(res.isEmpty)(res.toVector))

          override def onFailure(err: Throwable) = 
             err match
              case _: ZeroResultsException => p.success(None)
              case e: Throwable => p.failure(e)
      }

    p.future

  private def constructRows(columns: Vector[AddressComponentType])(res: Vector[Option[Vector[GeocodingResult]]]) =
    val headerRow = columns.map(_.toString.some)
    val dataRows = res.map { 
      // take only the first result for ambiguous addresses
      _.flatMap(_.lift(0))
       .map(r => columns.map(findAddressComponent(r)))
       .getOrElse(Vector.tabulate(columns.length)(_ => None))
    }

    headerRow +: dataRows

  private def findAddressComponent(result: GeocodingResult)(column: AddressComponentType) = 
    result.addressComponents.find {
      case c if c.types.exists(_ == column) => true 
      case otherwise => false
    }
    .map(_.longName)


object GeocodingService:
  import com.google.maps.model.AddressComponentType.*
  given Releasable[GeocodingService] with 
    def release(r: GeocodingService) = r.shutdown()
