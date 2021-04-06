package com.quantemplate.capitaliq

import java.time.LocalDate
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext

import com.quantemplate.capitaliq.domain.*
import com.quantemplate.capitaliq.qt.*

@main
def generateRevenueSheet() =
  given Config = Config.load()
  given ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")

  val httpService = HttpService()
  val revenueReport = RevenueReport(
    CapitalIQService(httpService), 
    QTService(httpService)
  )

  revenueReport.generateSpreadSheet(
    ids = Identifiers.loadFromResource(), 
    range = (
      LocalDate.of(1988, 12, 31), 
      LocalDate.of(2018, 12, 31)
    ),
    currency = "USD",
    orgId = "c-my-small-insuranc-ltdzfd",
    // datasetId = "d-33oxxtejxjb3rtirccilw6nm"
    datasetId = "d-mc6ao4re-zagaqpyipxnbvbf"
  )
