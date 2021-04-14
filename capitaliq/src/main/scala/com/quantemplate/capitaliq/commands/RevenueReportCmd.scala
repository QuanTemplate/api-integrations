package com.quantemplate.capitaliq.commands

import java.util.Calendar
import java.time.ZoneId
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import com.quantemplate.capitaliq.common.*
import com.quantemplate.capitaliq.domain.*
import com.quantemplate.capitaliq.qt.*

object RevenueReportCmd:
  def run(args: Array[String]) = 
    RevenueReportArgsParser.parse(args).map { config => 
      given Config = Config.load()
      given sys: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
      given ExecutionContext = sys.executionContext

      val logger = sys.log
      val httpService = HttpService()
      val revenueReport = RevenueReport(CapitalIQService(httpService), QTService(httpService))

      revenueReport
        .generateSpreadSheet(
          ids = config.identifiers
            .map(Identifiers(_*))
            .getOrElse {
              logger.info("Loading the Capital IQ identifiers from the STDIN")

              Identifiers.loadFromStdin()
            },
          range = (
            fromCalendar(config.from),
            fromCalendar(config.to)
          ),
          currency = config.currency,
          orgId = config.orgId,
          datasetId = config.datasetId
        ).onComplete { 
          case Failure(e) => 
            logger.error("Failed to generate the revenue report: {}", e.toString)
            Runtime.getRuntime.halt(1)

          case Success(_) =>
            Runtime.getRuntime.halt(0)
        }
    }

  private def fromCalendar(cal: Calendar) =
    cal.getTime.toInstant.atZone(ZoneId.systemDefault).toLocalDate
