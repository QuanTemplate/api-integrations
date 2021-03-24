package com.quantemplate.capitaliq

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }

class HttpService(using system: ActorSystem[_], conf: Config):
  given ExecutionContext = system.executionContext

  def sendRequest[A](req: A)(using Marshaller[A, RequestEntity]) =
    for 
      entity <- Marshal(req).to[RequestEntity]
      request = HttpRequest(
        method = HttpMethods.POST,
        uri = conf.endpoint,
        entity = entity,
        headers = Seq(
          Authorization(
            BasicHttpCredentials(
              conf.credentials.username, 
              conf.credentials.password
            )
          )
        ),
      )
      result <- Http().singleRequest(request)
    yield result

