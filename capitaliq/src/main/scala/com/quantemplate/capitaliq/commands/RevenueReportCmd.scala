package com.quantemplate.capitaliq.commands

import java.util.Calendar
import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.Future
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.util.{Failure, Success}
import scopt.OParser

import com.quantemplate.capitaliq.common.*
import com.quantemplate.capitaliq.domain.*
import com.quantemplate.capitaliq.qt.*

// OParser requires default args
case class RevenueReportCmd(
  orgId: String = "",
  datasetId: String = "",
  from: Calendar = Calendar.getInstance,
  to: Calendar = Calendar.getInstance,
  currency: String = "",
  identifiers: Option[Vector[String]] = None
)

object RevenueReportCmd:
  def run(args: Array[String]) = OParser.parse(parser, args, RevenueReportCmd()).map { config =>
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

  private lazy val builder = OParser.builder[RevenueReportCmd]
  private lazy val parser = 
    import builder.*

    OParser.sequence(
      programName("qt-capitaliq"),
      head("qt-capitaliq", "0.0.1"),
      cmd("generateRevenueReport")
        .children(
          opt[String]("orgId")
            .action((id, c) => c.copy(orgId = id))
            .required
            .text("Id of the organisation in the Quantemplate"),

          opt[String]("datasetId")
            .action((id, c) => c.copy(datasetId = id))
            .required
            .text("Id of the dataset in the Quantemplate"),

          opt[String]("currency")
            .action((currency, c) => c.copy(currency = currency))
            .required
            .text("Currency supported by the Capital IQ"),

          opt[Calendar]("from")
            .action((from, c) => c.copy(from = from))
            .required
            .text("Start date in the yyy-mm-dd format"),

          opt[Calendar]("to")
            .action((to, c) => c.copy(to = to))
            .required
            .text("End date in the yyy-mm-dd format"),

          opt[Seq[String]]("identifiers")
            .action((ids, c) => c.copy(identifiers = Some(ids.toVector)))
            .optional
            .text("Capital IQ identifiers")
        )
    )
