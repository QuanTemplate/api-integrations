package com.quantemplate.capitaliq.common

import scala.concurrent.{ExecutionContext, Future}
import java.io.ByteArrayOutputStream
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{RequestEntity, HttpRequest, HttpMethod, HttpMethods, HttpResponse, HttpEntity}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model.headers.Authorization
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import io.circe.{Encoder, Decoder}

class HttpService(using system: ActorSystem[_]):
  import HttpService.*

  given ExecutionContext = system.executionContext

  def get[B: Decoder](
    endpoint: String,
    auth: Option[Authorization] 
  ): Future[B] =
    for
      res <- GET(endpoint, auth)
      body <- getResponseBody(res)
      result <- Unmarshal(body).to[B]
    yield result

  def post[B: Decoder](
    endpoint: String, 
    req: RequestEntity, 
    auth: Option[Authorization]
  ): Future[B] =
    for
      res <- POST(endpoint, req, auth)
      body <- getResponseBody(res)
      result <- Unmarshal(body).to[B]
    yield result

  def post[A: Encoder, B: Decoder](
    endpoint: String, 
    req: A, 
    auth: Option[Authorization]
  ): Future[B] =
    for 
      entity <- Marshal(req).to[RequestEntity]
      res <- POST(endpoint, entity, auth)
      body <- getResponseBody(res)
      result <- Unmarshal(body).to[B]
    yield result

  def upload(
    endpoint: String, 
    bytes: Array[Byte], 
    auth: Option[Authorization] = None
  ): Future[UploadResponse] = 
    for
      res <- POST(endpoint, HttpEntity(bytes), auth)
      body <- getResponseBody(res)
    yield HttpService.UploadResponse(res.status.intValue, body.utf8String)

  private def getResponseBody(res: HttpResponse) = 
    res.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)

  private def POST(endpoint: String, entity: RequestEntity, auth: Option[Authorization]) = 
    Http().singleRequest(
      HttpRequest(
        method = HttpMethods.POST,
        uri = endpoint,
        entity = entity,
        headers = auth.map(Seq(_)).getOrElse(Seq.empty)
      )
    )

  private def GET(endpoint: String, auth: Option[Authorization]) = 
    Http().singleRequest(
      HttpRequest(
        method = HttpMethods.GET,
        uri = endpoint,
        headers = auth.map(Seq(_)).getOrElse(Seq.empty)
      )
    )

object HttpService:
  case class UploadResponse(statusCode: Int, body: String)
