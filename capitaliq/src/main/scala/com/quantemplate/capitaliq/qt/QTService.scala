package com.quantemplate.capitaliq.qt

import akka.actor.typed.ActorSystem
import io.circe.{Encoder, Decoder}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem
import cats.syntax.option.*
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import com.norbitltd.spoiwo.model.{Row, Sheet, Workbook}
// import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }

import com.quantemplate.capitaliq.{Config, HttpService}

class QTService(httpService: HttpService)(using system: ActorSystem[_], conf: Config):
  import QTService.*

  given ExecutionContext = system.executionContext
  lazy val logger = LoggerFactory.getLogger(getClass)

  def uploadDataset(workbook: Workbook) =
    import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions.*

    val stream: java.io.ByteArrayOutputStream = new java.io.ByteArrayOutputStream()
    workbook.writeToOutputStream(stream)

    val orgId = "c-my-small-insuranc-ltdzfd"
    val datasetId = "d-33oxxtejxjb3rtirccilw6nm"

    for
      tokenRes <- getToken()
      r <- httpService.upload(
            s"${conf.quantemplate.api.baseUrl}/external/v1/organisations/$orgId/datasets/$datasetId",
            stream,
            Authorization(
              OAuth2BearerToken(
               tokenRes.accessToken
              )
            ).some
          )

    yield ()

  def getToken(): Future[TokenResponse] = 
    httpService.POST2[TokenResponse](
      conf.quantemplate.auth.endpoint,
      FormData(
        "grant_type" -> "client_credentials",
        "client_id" -> conf.quantemplate.auth.clientId,
        "client_secret" -> conf.quantemplate.auth.clientSecret
      ).toEntity
    )

object QTService:
  // case class TokenRequest(
  //   grantType: String,
  //   clientId: String,
  //   clientSecret: String
  // )
  // object TokenRequest:
  //   given Encoder[TokenRequest] = Encoder.forProduct3("grant_type", "client_id", "client_secret") { t => 
  //     (t.grantType, t.clientId, t.clientSecret)
  //   }

  // opaque type AccessToken = String
  // object AccessToken:
    // def apply(str: String): AccessToken = str

  case class TokenResponse(accessToken: String)
  object TokenResponse:
    given Decoder[TokenResponse] = Decoder.forProduct1("access_token")(TokenResponse(_))


  // def upload(file_path, access_token, org_id, dataset_id):
  // url = f'{fabric_endpoint}/external/v1/organisations/{org_id}/datasets/{dataset_id}'
  // with open(file_path, mode='rb') as f:
  //   return requests.post(url, data = f, headers = {
  //     'Authorization': f'Bearer {access_token}'
  //   })