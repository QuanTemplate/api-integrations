package com.quantemplate.integrations.qt

import io.circe.{Encoder, Decoder}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem
import cats.syntax.option.given
import cats.syntax.apply.given
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }

import com.quantemplate.integrations.common.{View, Config, HttpService}
import akka.http.scaladsl.model.HttpEntity

class QTService(httpService: HttpService)(using ec: ExecutionContext, conf: Config.Quantemplate):
  import QTService.*
  
  lazy val logger = LoggerFactory.getLogger(getClass)

  private def datasetEndpoint(orgId: String, datasetId: String) = 
    s"${conf.api.baseUrl}/v1/organisations/$orgId/datasets/$datasetId"

  private def executionsEndpoints(orgId: String, pipelineId: String) =
    s"${conf.api.baseUrl}/v1/organisations/$orgId/pipelines/$pipelineId/executions"

  def executePipeline(orgId: String, pipelineId: String) =
    val endpoint = executionsEndpoints(orgId, pipelineId)

    for
      auth <- getToken().map(getTokenAuthHeaders)
      res <- httpService.post[PipelineExecutionResponse](endpoint, HttpEntity.Empty, auth)

    yield res

  def listExecutions(orgId: String, pipelineId: String) =
    val endpoint = executionsEndpoints(orgId, pipelineId)

    for
      auth <- getToken().map(getTokenAuthHeaders)
      res <- httpService.get(endpoint, auth)
    yield res

  def downloadPipelineOutput(
    orgId: String, 
    pipelineId: String, 
    executionId: String, 
    outputId: String
  ) =  
    val endpoint = s"${executionsEndpoints(orgId, pipelineId)}/$executionId/outputs/$outputId"

    for 
      auth <- getToken().map(getTokenAuthHeaders)
      res <- httpService.get(endpoint, auth)
    yield res match
      case HttpService.Response(200, Some(res)) => res 
      case res @ HttpService.Response(403, _) => 
        throw Forbidden("pipeline output download", res)
      case other => 
        throw UnexpectedError(other)

  def uploadDataset(view: View, orgId: String, datasetId: String) =
    val endpoint = datasetEndpoint(orgId, datasetId)

    for
      auth <- getToken().map(getTokenAuthHeaders)
      res <- httpService.post(endpoint, view.toBytes, auth)

    yield res match 
      case res @ HttpService.Response(200, _) => ()
      case res @ HttpService.Response(403, _) => 
        throw Forbidden("dataset upload", res)
      case other => 
        throw UnexpectedError(other)

  def downloadDataset(orgId: String, datasetId: String) =
    val endpoint = datasetEndpoint(orgId, datasetId)

    for
      auth <- getToken().map(getTokenAuthHeaders)
      res <- httpService.get(endpoint, auth)
      
    yield res match
      case HttpService.Response(200, Some(res)) => res 
      case res @ HttpService.Response(403, _) => 
        throw Forbidden("dataset download", res)
      case other => 
        throw UnexpectedError(other)

  private def getTokenAuthHeaders(res: TokenResponse) =
    Authorization(OAuth2BearerToken(res.accessToken)).some

  private def getToken(): Future[TokenResponse] = 
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

  case class PipelineExecutionResponse(
    executionId: String,
    runNumber: Int,
    version: Int,
  )
  object PipelineExecutionResponse:
    given Decoder[PipelineExecutionResponse] = Decoder { c => 
      (
        c.get[String]("executionId"),
        c.get[Int]("runNumber"),
        c.get[Int]("version")
      ).mapN(PipelineExecutionResponse.apply)
    }

  type ExecutionStatus =  "Started" | "Succeeded" | "Failed" | "Canceled"
  given Decoder[ExecutionStatus] = Decoder.decodeString
    .ensure({ case e: ExecutionStatus => true; case other => false }, "Not a valid status")
    .map(_.asInstanceOf[ExecutionStatus]) 
  
  case class Execution(
     id: String,
     status: ExecutionStatus
  )
  object Execution:
    given Decoder[Execution] = Decoder { c => 
      
      (
        c.get[String]("id"),
        c.get[ExecutionStatus]("status"),
      ).mapN(Execution.apply)
      
    }
