package com.quantemplate.integrations.capitaliq

import munit.FunSuite
import io.circe.Json
import io.circe.syntax.given
import cats.syntax.option.given

import CapitalIQ.*
import CapitalIQ.Properties.*
import com.quantemplate.integrations.capitaliq.CapitalIQ.Mnemonic.IQ_COMPANY_NAME_LONG
import com.quantemplate.integrations.capitaliq.CapitalIQ.Mnemonic.IQ_TOTAL_REV

class CapitalIQSpec extends FunSuite:
  test("Request marshalling") {
    val jsonRequest = Request(
      inputRequests = Vector(
        IQ_COMPANY_NAME_LONG(
          Identifier("IQ12345")
        ),
        IQ_TOTAL_REV(
          IQ_TOTAL_REV.Fn.GDSP(
            currencyId = "USD",
            asOfDate = "05/05/2021".some,
            periodType = ("IQ_FY" back 20).some,
            restatementTypeId = None,
            filingMode = "F".some
          ),
          Identifier("IQ12345")
        )
      )
    ).asJson

    assertEquals(
      jsonRequest,
      Json.obj(
        "inputRequests" -> Json.arr(
          Json.obj(
            "function" -> "GDSP".asJson,
            "identifier" -> "IQ12345".asJson,
            "mnemonic" -> "IQ_COMPANY_NAME_LONG".asJson
          ),
          Json.obj(
            "function" -> "GDSP".asJson,
            "identifier" -> "IQ12345".asJson,
            "mnemonic" -> "IQ_TOTAL_REV".asJson,
            "properties" -> Json.obj(
              "currencyId" -> "USD".asJson,
              "periodType" -> "IQ_FY-20".asJson,
              "asOfDate" -> "05/05/2021".asJson,
              "restatementTypeId" -> Json.Null,
              "filingMode" -> "F".asJson,
              "consolidatedFlag" -> Json.Null,
              "currencyConversionModeId" -> Json.Null
            )
          )
        )
      )
    )
  }
