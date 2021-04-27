package com.quantemplate.capitaliq.commands.revenuereport

import java.time.LocalDate

import com.quantemplate.capitaliq.domain.CapitalIQ.Identifier

case class CmdConfig(
  orgId: String,
  datasetId: String,
  from: LocalDate,
  to: LocalDate,
  currency: String,
  identifiers: Vector[Identifier]
  // columns
)
