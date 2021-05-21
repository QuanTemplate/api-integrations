package com.quantemplate.integrations.common

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

// TODO: improve error handling by wrapping responses in Either / Try
class HttpService(using system: ActorSystem[_]):
  import HttpService.*

  given ExecutionContext = system.executionContext

  def getRaw(
    endpoint: String,
    auth: Option[Authorization] 
  ): Future[Response] =
    for
      res <- GET(endpoint, auth)
      body <- getResponseBody(res)
    yield HttpService.Response(res.status.intValue, body.map(_.utf8String))

  def get[B: Decoder](
    endpoint: String,
    auth: Option[Authorization] 
  ): Future[B] =
    for
      res <- GET(endpoint, auth)
      body <- getResponseBody(res)
      // _ = println(body.map(_.utf8String))
      result <- Unmarshal(body.get).to[B]
    yield result

  def post[B: Decoder](
    endpoint: String, 
    req: RequestEntity, 
    auth: Option[Authorization]
  ): Future[B] =
    for
      res <- POST(endpoint, req, auth)
      body <- getResponseBody(res)
      //  _ = println(body.map(_.utf8String))
      result <- Unmarshal(body.get).to[B]
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
      result <- Unmarshal(body.get).to[B]
    yield result

  def post(
    endpoint: String, 
    bytes: Array[Byte], 
    auth: Option[Authorization]
  ): Future[Response] = 
    for
      res <- POST(endpoint, HttpEntity(bytes), auth)
      body <- getResponseBody(res)
    yield HttpService.Response(res.status.intValue, body.map(_.utf8String))

  private def getResponseBody(res: HttpResponse) = 
    res.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(Option(_)) // could return null

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
  case class Response(statusCode: Int, body: Option[String])
