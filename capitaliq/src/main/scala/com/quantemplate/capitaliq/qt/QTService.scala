package com.quantemplate.capitaliq.qt

import io.circe.{Encoder, Decoder}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem
import cats.syntax.option.*
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }

import com.quantemplate.capitaliq.common.{View, Config, HttpService}

class QTService(httpService: HttpService)(using ec: ExecutionContext, conf: Config):
  import QTService.*
  
  lazy val logger = LoggerFactory.getLogger(getClass)

  private def datasetEndpoint(orgId: String, datasetId: String) = 
    s"${conf.quantemplate.api.baseUrl}/v1/organisations/$orgId/datasets/$datasetId"


  def uploadDataset(view: View, orgId: String, datasetId: String) =
    val endpoint = datasetEndpoint(orgId, datasetId)

    for
      tokenRes <- getToken()
      res <- httpService.post(
        endpoint, 
        view.toBytes, 
        Authorization(OAuth2BearerToken(tokenRes.accessToken)).some
      )

    yield res match 
      case HttpService.Response(200, _) => ()
      case HttpService.Response(403, _) => 
        throw DatasetUploadError("403: Did you share the dataset with whole org?")
      case other => 
        throw DatasetUploadError(s"${other.statusCode}: ${other.body}")

  def downloadDataset(orgId: String, datasetId: String) =
    val endpoint = datasetEndpoint(orgId, datasetId)

    for
      tokenRes <- getToken()
      res <- httpService.get(
        endpoint,
        Authorization(OAuth2BearerToken(tokenRes.accessToken)).some
      )
    yield res


  def getToken(): Future[TokenResponse] = 
    httpService.post[TokenResponse](
      conf.quantemplate.auth.endpoint,
      FormData(
        "grant_type" -> "client_credentials",
        "client_id" -> conf.quantemplate.auth.clientId,
        "client_secret" -> conf.quantemplate.auth.clientSecret
      ).toEntity,
      None
    )
    

object QTService:
  case class TokenResponse(accessToken: String)
  object TokenResponse:
    given Decoder[TokenResponse] = Decoder.forProduct1("access_token")(TokenResponse(_))
