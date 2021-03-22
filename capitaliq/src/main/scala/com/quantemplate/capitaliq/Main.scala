package com.quantemplate.capitaliq

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import akka.util.ByteString

object Main extends App {
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  implicit val ec: ExecutionContext = system.executionContext

  Http()
    .singleRequest(HttpRequest(uri = "https://jsonplaceholder.typicode.com/todos/1"))
    .onComplete {
      case Failure(exception) => sys.error(s"Error: $exception")
      case Success(response) =>
        response.entity.dataBytes.runFold(ByteString(""))(_ ++ _) foreach { body =>
          system.log.info("Got response, body: " + body.utf8String)
        }

        system.terminate()
    }

}
