package com.quantemplate.capitaliq

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext


@main
def run() =
  given Config = Config.load()
  given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  given ExecutionContext = system.executionContext

  val httpService = HttpService()
  val capitaliqService = CapitalIQService(httpService)

  capitaliqService
    .getRevenueReport()
    .onComplete(_ => system.terminate())
