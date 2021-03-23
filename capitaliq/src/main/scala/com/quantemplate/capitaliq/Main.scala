package com.quantemplate.capitaliq

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import akka.util.ByteString
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes

@main
def run() =
  given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  given ExecutionContext = system.executionContext

  Http()
    .singleRequest(
      HttpRequest(
        method = HttpMethods.POST,
        uri = "https://jsonplaceholder.typicode.com/posts",
        entity = HttpEntity(
          ContentTypes.`application/json`,
          raw"""{ "title": "foo", "body": "bar", "userId": 1 }"""
        )
      )
    )
    .onComplete {
      case Failure(exception) => sys.error(s"Error: $exception")
      case Success(response) =>
        response.entity.dataBytes.runFold(ByteString(""))(_ ++ _) foreach { body =>
          system.log.info("Got response, body: " + body.utf8String)
        }

        system.terminate()
    }

