package com.quantemplate.capitaliq.commands.mutlidatapointreport

import java.time.LocalDate

import com.quantemplate.capitaliq.domain.CapitalIQ.Identifier

case class CmdConfig(
  orgId: String,
  datasetId: String,
  currency: String,
  date: LocalDate,
  identifiers: Vector[Identifier],
  columns: Vector[ColumnDef]
)
