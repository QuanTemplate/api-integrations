package com.quantemplate.capitaliq

import java.util.Calendar
import java.time.LocalDateTime
import java.time.ZoneId
import scala.concurrent.Future
import scopt.OParser
import org.slf4j.LoggerFactory

import com.quantemplate.capitaliq.domain.*

object Main:
  lazy val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) = args match
    case Array("generateRevenueReport", _*) => Cmd.RevenueReport.run(args)
    case _ => 
      logger.error("Unsupported method")
      System.exit(1)

trait Cmd
object Cmd:
  case class RevenueReport(
    orgId: String = "",
    datasetId: String = "",
    from: Calendar = Calendar.getInstance,
    to: Calendar = Calendar.getInstance,
    currency: String = "",
    identifiers: Option[Vector[String]] = None
  ) extends Cmd
  object RevenueReport:
    private lazy val logger = LoggerFactory.getLogger(getClass)

    def run(args: Array[String]) =
       OParser.parse(parser, args, RevenueReport()).map { config => 
          withRevenueReport {
            _.generateSpreadSheet(
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
            )
          }
        }

    private lazy val builder = OParser.builder[RevenueReport]
    private lazy val parser = {
      import builder.*

      OParser.sequence(
        programName("qt-capitaliq"),
        head("qt-capitaliq", "0.0.1"),
        cmd("generateRevenueReport")
          .children(
            opt[String]("orgId")
              .action((id, c) => c.copy(orgId = id))
              .required,
            opt[String]("datasetId")
              .action((id, c) => c.copy(datasetId = id))
              .required,
            opt[String]("currency")
              .action((currency, c) => c.copy(currency = currency))
              .required,
            opt[Calendar]("from")
              .action((from, c) => c.copy(from = from))
              .text("start date in the yyy-mm-dd format")
              .required,
            opt[Calendar]("to")
              .action((to, c) => c.copy(to = to))
              .text("end date in the yyy-mm-dd format")
              .required,
            opt[Seq[String]]("identifiers")
              .action((ids, c) => c.copy(identifiers = Some(ids.toVector)))
              .optional
          )
      )
    }

    private def fromCalendar(cal: Calendar) =
      cal.getTime.toInstant.atZone(ZoneId.systemDefault).toLocalDate
