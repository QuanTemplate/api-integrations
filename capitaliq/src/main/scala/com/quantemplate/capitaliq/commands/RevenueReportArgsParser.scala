package com.quantemplate.capitaliq.commands

import java.util.Calendar
import scopt.OParser

object RevenueReportArgsParser:
  // OParser requires default args
  case class Config(
    orgId: String = "",
    datasetId: String = "",
    from: Calendar = Calendar.getInstance,
    to: Calendar = Calendar.getInstance,
    currency: String = "",
    identifiers: Option[Vector[String]] = None
  )
  
  def parse(args: Array[String]) = OParser.parse(parser, args, Config()) 

  private lazy val builder = OParser.builder[Config]
  private lazy val parser = 
    import builder.*

    OParser.sequence(
      programName("capitaliq-qt integration"),
      head("capitaliq-qt", "0.0.1"),
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
