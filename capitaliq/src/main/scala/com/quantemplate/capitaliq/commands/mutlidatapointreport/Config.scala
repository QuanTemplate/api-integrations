package com.quantemplate.capitaliq.commands.mutlidatapointreport

import com.quantemplate.capitaliq.domain.CapitalIQ.Identifier

case class CmdConfig(
  orgId: String,
  datasetId: String,
  identifiers: Vector[Identifier]
  // columns
)
