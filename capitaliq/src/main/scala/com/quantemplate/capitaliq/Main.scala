package com.quantemplate.capitaliq

import java.time.LocalDate

import com.quantemplate.capitaliq.domain.Identifiers

// TODO: build proper CLI
@main
def generateRevenueSheet = withRevenueReport {
  _.generateSpreadSheet(
    ids = Identifiers.loadFromResource(),
    range = (
      LocalDate.of(1988, 12, 31), 
      LocalDate.of(2018, 12, 31)
    ),
    currency = "USD",
    orgId = "c-my-small-insuranc-ltdzfd",
    datasetId = "d-e4tf3yyxerabcvicidv5oyey"
  )
}
