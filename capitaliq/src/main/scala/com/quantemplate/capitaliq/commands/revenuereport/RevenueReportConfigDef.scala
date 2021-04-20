package com.quantemplate.capitaliq.commands.revenuereport

import java.time.LocalDate
import io.circe.Decoder
import io.circe.yaml.{parser as ymlParser}
import io.circe.syntax.given
import cats.syntax.apply.given

import com.quantemplate.capitaliq.domain.CapitalIQ.Identifier
import com.quantemplate.capitaliq.commands.ConfigDef

case class RevenueReportConfigDef(
  orgId: String,
  datasetId: String,
  currency: String,
  from: LocalDate,
  to: LocalDate,
  identifiers: Option[RevenueReportConfigDef.Identifiers]
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
      c.get[Option[RevenueReportConfigDef.Identifiers]]("identifiers")
    ).mapN(RevenueReportConfigDef.apply)
  }

  case class Identifiers(
    local: Option[String],
    dataset: Option[String],
    inline: Option[Vector[Identifier]]
  )

  object Identifiers:
    given Decoder[Identifiers] = Decoder { c => 
      (
        c.get[Option[String]]("local"),
        c.get[Option[String]]("dataset"),
        c.get[Option[Vector[Identifier]]]("inline")
      ).mapN(Identifiers.apply)
    }
  