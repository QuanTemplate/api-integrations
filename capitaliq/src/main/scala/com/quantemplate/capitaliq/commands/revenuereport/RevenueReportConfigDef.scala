package com.quantemplate.capitaliq.commands.revenuereport

import java.time.LocalDate
import io.circe.Decoder
import io.circe.syntax.given
import cats.syntax.apply.given

import com.quantemplate.capitaliq.domain.CapitalIQ.Identifier
import com.quantemplate.capitaliq.commands.ConfigDef
import com.quantemplate.capitaliq.commands.IdentifierLoader.*

case class RevenueReportConfigDef(
  orgId: String,
  datasetId: String,
  currency: String,
  from: LocalDate,
  to: LocalDate,
  identifiers: Option[IdentifiersConf]
) extends ConfigDef:
  def toCmdConfig(loadedIds: => Vector[Identifier]) = CmdConfig(
    orgId = orgId,
    datasetId = datasetId,
    from = from,
    to = to,
    currency = currency,
    identifiers = loadedIds
  )

object RevenueReportConfigDef:
  given Decoder[RevenueReportConfigDef] = Decoder { c => 
    (
      c.get[String]("orgId"),
      c.get[String]("datasetId"),
      c.get[String]("currency"),
      c.get[LocalDate]("from"),
      c.get[LocalDate]("to"),
      c.get[Option[IdentifiersConf]]("identifiers")
    ).mapN(RevenueReportConfigDef.apply)
  }
