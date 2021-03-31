package com.quantemplate.capitaliq

import java.time.LocalDate
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext

import com.quantemplate.capitaliq.domain.*

@main
def generateRevenueSheet(filePath: String) =
  given Config = Config.load()
  given ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")

  val revenueReport = RevenueReport(CapitalIQService(HttpService()))

  revenueReport.generateSpreadSheet(
    ids = Identifiers.load(), 
    range = (
      LocalDate.of(1988, 12, 31), 
      LocalDate.of(2018, 12, 31)
    ),
    filePath = filePath
  )
