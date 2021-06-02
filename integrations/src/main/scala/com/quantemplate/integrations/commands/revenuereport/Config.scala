package com.quantemplate.integrations.commands.revenuereport

import java.time.LocalDate

import com.quantemplate.integrations.capitaliq.CapitalIQ.Identifier

case class CmdConfig(
  orgId: String,
  datasetId: String,
  from: LocalDate,
  to: LocalDate,
  currency: String,
  identifiers: Vector[Identifier]
)
