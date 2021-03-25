package com.quantemplate.capitaliq

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{RequestEntity, HttpRequest, HttpMethod, HttpMethods, HttpResponse}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model.headers.Authorization
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import io.circe.{Encoder, Decoder}

class HttpService(using system: ActorSystem[_]):
  given ExecutionContext = system.executionContext

  def POST[A: Encoder, B: Decoder](
    endpoint: String, 
    req: A, 
    auth: Option[Authorization] = None
  ): Future[B] =
    for 
      entity <- Marshal(req).to[RequestEntity]
      request = HttpRequest(
        method = HttpMethods.POST,
        uri = endpoint,
        entity = entity,
        headers = auth.map(Seq(_)).getOrElse(Seq.empty)
      )
      res <- Http().singleRequest(request)
      str <- res.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
      result <- Unmarshal(str).to[B]
    yield result
