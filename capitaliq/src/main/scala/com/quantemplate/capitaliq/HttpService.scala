package com.quantemplate.capitaliq

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{RequestEntity, HttpRequest, HttpMethod, HttpMethods, HttpResponse}
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model.headers.Authorization

class HttpService(using system: ActorSystem[_]):
  given ExecutionContext = system.executionContext

  def POST[A](
    endpoint: String, 
    req: A, 
    auth: Option[Authorization] = None
  )(using Marshaller[A, RequestEntity]): Future[HttpResponse] =
    for 
      entity <- Marshal(req).to[RequestEntity]
      request = HttpRequest(
        method = HttpMethods.POST,
        uri = endpoint,
        entity = entity,
        headers = auth.map(Seq(_)).getOrElse(Seq.empty)
      )
      result <- Http().singleRequest(request)
    yield result

