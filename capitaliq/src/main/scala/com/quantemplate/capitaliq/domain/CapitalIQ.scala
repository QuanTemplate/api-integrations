package com.quantemplate.capitaliq.domain

object CapitalIQ:
  // enum IQFunction:
  //   /** Retrieves a single data point for a point in time */
  //   case GDSP()

  //   /** Retrieves an array of values for the most current availability of content either end of day ot intra-day */
  //   case GDSPV()

  //   /** Retrieves a set of values that belong to a specific group using different mnemonics*/
  //   case GDSG()

  //   /** Retrieves historical values for a mnemonic over a range of dates */
  //   case GDSHE()

  //   /** Retrieves an array or set of values over a historical range of dates */
  //   case GDSHV()

  //   /** Retrieves historical values for a mnemonic over a range of dates with a specific frequency */
  //   case GDST()

  // import IQFunction.*

  opaque type Identifier = String
  object Identifier:
    def apply(s: String): Identifier = s

  object Properties:
    opaque type RelativePeriod = String
    object RelativePeriod:
      type Base = 
        "IQ_CH"    | // Calendar half
        "IQ_CQ"    | // Calendar quarter
        "IQ_CY"    | // Calendar year
        "IQ_FH"    | // Fiscal half
        "IQ_FQ"    | // Fiscal quarter
        "IQ_FY"    | // Fiscal year
        "IQ_LTM"   | // Last 12 months
        "IQ_NTM"   | // Next 12 months
        "IQ_MONTH" | // Last completed month
        "IQ_YTD"     // Year-to-Date

    extension (period: RelativePeriod.Base)
      def back(n: Int): MarkedPeriod = s"${period}-${n}"
      def forward(n: Int): MarkedPeriod = s"${period}+${n}"

    opaque type MarkedPeriod = String

    type MetaDataTag = "FiscalYear" | "AsOfDate"
    type FilingMode = "P" | "F"
    type ConsolidatedFlag = "CON" | "PAR" | "UNC"
    type CurrencyConversionModeId = "Historical" | "SpotRate"

  import Properties.*


  sealed trait Mnemonic
  object Mnemonic:
    // todo: reduce boilerplate of shared props with generic tuples: 
    // https://www.scala-lang.org/2021/02/26/tuples-bring-generic-programming-to-scala-3.html

    case class IQ_TOTAL_REV(
      properties: IQ_TOTAL_REV.Props,
      identifier: Identifier
    ) extends Mnemonic
    object IQ_TOTAL_REV:
      enum Props:
        case GDSP(
          currencyId: String,
          periodType: MarkedPeriod,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )

        case GDSHE(
          currencyId: String,
          periodType: MarkedPeriod,
          metaDataTag: Option[MetaDataTag] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )

  val xx = Mnemonic.IQ_TOTAL_REV(
    properties = Mnemonic.IQ_TOTAL_REV.Props.GDSHE(
        currencyId = "USD",
        periodType = "IQ_FY" back 31,
        metaDataTag = Some("FiscalYear")
    ),
    identifier = Identifier("IQ121238")
  )