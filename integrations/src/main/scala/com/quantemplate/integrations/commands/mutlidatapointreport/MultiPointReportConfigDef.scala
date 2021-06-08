package com.quantemplate.integrations.commands.mutlidatapointreport

import java.time.LocalDate
import io.circe.Decoder
import io.circe.syntax.given
import cats.syntax.apply.given

import com.quantemplate.integrations.commands.ConfigDef
import com.quantemplate.integrations.capitaliq.CapitalIQ.Identifier
import com.quantemplate.integrations.commands.IdentifierLoader.*

case class MultiPointReportConfigDef(
    orgId: String,
    datasetId: String,
    currency: String,
    date: LocalDate,
    identifiers: Option[IdentifiersConf],
    columns: Vector[ColumnDef]
) extends ConfigDef
    derives Decoder:
  def toCmdConfig(loadedIds: => Vector[Identifier]): CmdConfig = CmdConfig(
    orgId = orgId,
    datasetId = datasetId,
    currency = currency,
    date = date,
    identifiers = loadedIds,
    columns = columns
  )

case class ColumnDef(mnemonicId: String, header: String)
object ColumnDef:
  given Decoder[ColumnDef] =
    Decoder[String].map(m => ColumnDef(m, m)) or
      Decoder { c =>
        (c.get[String]("mnemonic"), c.get[String]("header")).mapN(ColumnDef.apply)
      }
