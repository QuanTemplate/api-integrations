package com.quantemplate.integrations.commands.revenuereport

import java.time.LocalDate
import io.circe.Decoder
import io.circe.syntax.given
import cats.syntax.apply.given

import com.quantemplate.integrations.capitaliq.CapitalIQ.Identifier
import com.quantemplate.integrations.commands.ConfigDef
import com.quantemplate.integrations.commands.IdentifierLoader.*

case class RevenueReportConfigDef(
    orgId: String,
    datasetId: String,
    currency: String,
    from: LocalDate,
    to: LocalDate,
    identifiers: Option[IdentifiersConf]
) extends ConfigDef
    derives Decoder:
  def toCmdConfig(loadedIds: => Vector[Identifier]): CmdConfig = CmdConfig(
    orgId = orgId,
    datasetId = datasetId,
    from = from,
    to = to,
    currency = currency,
    identifiers = loadedIds
  )
