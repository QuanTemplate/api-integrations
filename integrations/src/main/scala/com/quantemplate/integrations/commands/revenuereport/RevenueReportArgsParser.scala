package com.quantemplate.integrations.commands.revenuereport

import java.util.Calendar
import java.time.ZoneId
import scopt.OParser

import com.quantemplate.integrations.capitaliq.CapitalIQ.Identifier
import com.quantemplate.integrations.capitaliq.Identifiers

object RevenueReportArgsParser:
  case class CliConfig(
      orgId: String = "",
      datasetId: String = "",
      from: Calendar = Calendar.getInstance,
      to: Calendar = Calendar.getInstance,
      currency: String = "",
      identifiers: Option[Vector[String]] = None
  ):
    def toCmdConfig(fallbackIds: => Vector[Identifier]): CmdConfig = CmdConfig(
      orgId = orgId,
      datasetId = datasetId,
      from = fromCalendar(from),
      to = fromCalendar(to),
      currency = currency,
      identifiers = identifiers.map(Identifiers(_*)).getOrElse(fallbackIds)
    )

    private def fromCalendar(cal: Calendar) =
      cal.getTime.toInstant.atZone(ZoneId.systemDefault).toLocalDate

  def parse(args: Array[String]): Option[CliConfig] =
    OParser.parse(parser, args, CliConfig())

  private lazy val builder = OParser.builder[CliConfig]
  private lazy val parser =
    import builder.*

    OParser.sequence(
      programName("qt-integrations"),
      cmd(revenueReportCmdName)
        .text(
          "Generates a revenue report from CapitalIQ data and uploads it to the Quantemplate dataset"
        )
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
