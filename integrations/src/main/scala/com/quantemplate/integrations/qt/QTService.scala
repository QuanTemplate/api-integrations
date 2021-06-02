package com.quantemplate.integrations.qt

import io.circe.{Encoder, Decoder}
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem
import cats.syntax.option.given
import akka.http.scaladsl.model.{HttpEntity, FormData}
import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }

import com.quantemplate.integrations.common.{View, Config, HttpService}
import com.quantemplate.integrations.common.HttpService.*
import com.quantemplate.integrations.qt.QTModels.*


class QTService(httpService: HttpService)(using ec: ExecutionContext, conf: Config.Quantemplate):  
  lazy val logger = LoggerFactory.getLogger(getClass)
  lazy val endpoints = QTEndpoints(conf.api.baseUrl)

  def executePipeline(orgId: String, pipelineId: String): Future[PipelineExecutionResponse] =
    val endpoint = endpoints.executions(orgId, pipelineId)

    for
      auth <- getToken().map(getTokenAuthHeaders)
      res <- httpService.post[PipelineExecutionResponse](endpoint, HttpEntity.Empty, auth)
    yield res

  def listExecutions(orgId: String, pipelineId: String): Future[Vector[Execution]] =
    val endpoint = endpoints.executions(orgId, pipelineId)

    for
      auth <- getToken().map(getTokenAuthHeaders)
      res <- httpService.get[Vector[Execution]](endpoint, auth)
    yield res

  def downloadPipelineOutput(
    orgId: String, 
    pipelineId: String, 
    executionId: String, 
    outputId: String
  ): Future[String] =  
    val endpoint = endpoints.executionsOutput(orgId, pipelineId, executionId, outputId)

    for 
      auth <- getToken().map(getTokenAuthHeaders)
      res <- httpService.getRaw(endpoint, auth)
    yield res match
      case Response(200, Some(res)) => res
      case res @ Response(404, _) => 
        throw NotFound("pipeline output", res)
      case res @ Response(403, _) => 
        throw Forbidden("pipeline output download", res)
      case other => 
        throw UnexpectedError(other)

  def uploadDataset(view: View, orgId: String, datasetId: String): Future[Unit] =
    val endpoint = endpoints.dataset(orgId, datasetId)

    for
      auth <- getToken().map(getTokenAuthHeaders)
      res <- httpService.post(endpoint, view.toBytes, auth)
    yield res match 
      case Response(200, _) => ()
      case res @ Response(403, _) => 
        throw Forbidden("dataset upload", res)
      case other => 
        throw UnexpectedError(other)

  def downloadDataset(orgId: String, datasetId: String): Future[String] =
    val endpoint = endpoints.dataset(orgId, datasetId)

    for
      auth <- getToken().map(getTokenAuthHeaders)
      res <- httpService.getRaw(endpoint, auth) 
    yield res match
      case Response(200, Some(res)) => res 
      case res @ Response(403, _) => 
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

private class QTEndpoints(baseUrl: String):
  def dataset(orgId: String, datasetId: String) = 
    s"${baseUrl}/v1/organisations/$orgId/datasets/$datasetId"

  def executions(orgId: String, pipelineId: String) =
    s"${baseUrl}/v1/organisations/$orgId/pipelines/$pipelineId/executions"

  def executionsOutput(
    orgId: String, 
    pipelineId: String, 
    executionId: String, 
    outputId: String
  ) =
    s"${executions(orgId, pipelineId)}/$executionId/outputs/$outputId"
