package com.quantemplate.integrations.qt

import io.circe.{Encoder, Decoder}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem
import cats.syntax.option.*
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }

import com.quantemplate.integrations.common.{View, Config, HttpService}

class QTService(httpService: HttpService)(using ec: ExecutionContext, conf: Config.Quantemplate):
  import QTService.*
  
  lazy val logger = LoggerFactory.getLogger(getClass)

  private def datasetEndpoint(orgId: String, datasetId: String) = 
    s"${conf.api.baseUrl}/v1/organisations/$orgId/datasets/$datasetId"


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
      case res @ HttpService.Response(200, _) => ()
      case res @ HttpService.Response(403, _) => 
        throw Forbidden("dataset upload", res)
      case other => 
        throw UnexpectedError(other)

  def downloadDataset(orgId: String, datasetId: String) =
    val endpoint = datasetEndpoint(orgId, datasetId)

    for
      tokenRes <- getToken()
      res <- httpService.get(
        endpoint,
        Authorization(OAuth2BearerToken(tokenRes.accessToken)).some
      )
      
    yield res match
      case HttpService.Response(200, Some(res)) => res 
      case res @ HttpService.Response(403, _) => 
        throw Forbidden("dataset download", res)
      case other => 
        throw UnexpectedError(other)

  def getToken(): Future[TokenResponse] = 
    httpService.post[TokenResponse](
      conf.auth.endpoint,
      FormData(
        "grant_type" -> "client_credentials",
        "client_id" -> conf.auth.clientId,
        "client_secret" -> conf.auth.clientSecret
      ).toEntity,
      None
    )
object QTService:
  case class TokenResponse(accessToken: String)
  object TokenResponse:
    given Decoder[TokenResponse] = Decoder.forProduct1("access_token")(TokenResponse(_))