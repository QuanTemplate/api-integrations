package com.quantemplate.capitaliq

import java.time.LocalDate
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import org.slf4j.LoggerFactory

import com.quantemplate.capitaliq.domain.*
import com.quantemplate.capitaliq.qt.*

@main
def generateRevenueSheet() =
  given Config = Config.load()
  given sys: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  given ExecutionContext = sys.executionContext

  val logger = LoggerFactory.getLogger(getClass)

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
    // datasetId = "d-33oxxtejxjb3rtirccilw6nm" // capital-iq
    // datasetId = "d-mc6ao4re-zagaqpyipxnbvbf" // capital-iq2
    // datasetId = "d-4jtpe3fr3rebhsgh5poyzqsd" // capitaliq-3
    datasetId = "d-elfm3phujbbztumbhukxh4oe" // capitaliq-4
  ).onComplete { 
     case Failure(e) => 
      logger.error("Uncaught exception while generating the spreadsheet: {}", e.toString)
      Runtime.getRuntime.halt(1)

     case Success(_) =>
      Runtime.getRuntime.halt(0)
  }
  
