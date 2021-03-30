package com.quantemplate.capitaliq

import java.time.LocalDate
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext

import com.quantemplate.capitaliq.domain.*

@main
def run() =
  given Config = Config.load()
  given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  given ExecutionContext = system.executionContext

  val httpService = HttpService()
  val capitaliqService = CapitalIQService(httpService)
  val revenueReport = RevenueReport(capitaliqService)

  revenueReport.generateSpreadSheet(
    Identifiers.load(), 
    range = (
      LocalDate.of(1988, 12, 31), 
      LocalDate.of(2018, 12, 31)
    )
  )
