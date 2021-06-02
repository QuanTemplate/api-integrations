package com.quantemplate.integrations.commands.mutlidatapointreport

import java.time.LocalDate

import com.quantemplate.integrations.capitaliq.CapitalIQ.Identifier

case class CmdConfig(
  orgId: String,
  datasetId: String,
  currency: String,
  date: LocalDate,
  identifiers: Vector[Identifier],
  columns: Vector[ColumnDef]
)
