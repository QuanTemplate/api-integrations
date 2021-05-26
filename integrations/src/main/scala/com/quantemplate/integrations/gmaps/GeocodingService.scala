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
  import GeocodingService.*
  
  private lazy val context = GeoApiContext.Builder()
    .apiKey(conf.apiKey)
    .build()

  def shutdown() = context.shutdown()

  def getGeocodedRows(addresses: Vector[String])(using ExecutionContext) = 
    addresses
      .map(geocode)
      .sequence
      .map(constructRows)
   
  def geocode(address: String): Future[Option[Vector[GeocodingResult]]] = 
    val p = Promise[Option[Vector[GeocodingResult]]]()

    GeocodingApi
      .newRequest(context)
      .address(address)
      .setCallback {
        new PendingResult.Callback:
          override def onResult(res: Array[GeocodingResult]) =
            // `ZeroResult` responses are not treated as errors by the API
            p.success(Option.unless(res.isEmpty)(res.toVector))

          override def onFailure(err: Throwable) = 
             err match
              case _: ZeroResultsException => p.success(None)
              case e: Throwable => p.failure(e)
      }

    p.future

  private def constructRows(res: Vector[Option[Vector[GeocodingResult]]]) =
    val headerRow =
      Vector("latitude", "longitude", "place_id", "formatted_address").map(_.some) concat
      dynamicAddressComponents.map(_.toString.some)
                    
    val dataRows = res.map { 
      // ~ taking only the first result for ambiguous addresses
      // this should be smarter for a non-POC
      _.flatMap(_.lift(0))
       .map { r =>
          val loc = r.geometry.location
        
           Vector(loc.lat, loc.lng, r.placeId, r.formattedAddress).map(_.toString.some) concat
          dynamicAddressComponents.map(findAddressComponent(r)) 
  
       }
       .getOrElse(Vector.tabulate(dynamicAddressComponents.length)(_ => None))
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

  lazy val dynamicAddressComponents = Vector(
    STREET_NUMBER,
    ROUTE,
    LOCALITY,
    ADMINISTRATIVE_AREA_LEVEL_3,
    ADMINISTRATIVE_AREA_LEVEL_2,
    ADMINISTRATIVE_AREA_LEVEL_1,
    COUNTRY,
    POSTAL_CODE,
    POSTAL_CODE_SUFFIX
  )
