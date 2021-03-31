package com.quantemplate.capitaliq.domain

import io.circe.{ Encoder, Decoder, Json, DecodingFailure }
import io.circe.syntax.*
import cats.syntax.traverse.{*, given}
import cats.instances.vector.{*, given}

object CapitalIQ:
  opaque type Identifier = String
  object Identifier:
    def apply(s: String): Identifier = s
  extension (i: Identifier)
    def unwrap: String = i

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
      def apply(str: Base): RelativePeriod = str

    extension (period: RelativePeriod.Base)
      def marked: MarkedPeriod = period
      def back(n: Int): MarkedPeriod = s"${period}-${n}"
      def forward(n: Int): MarkedPeriod = s"${period}+${n}"

    opaque type MarkedPeriod = String
    extension (i: MarkedPeriod)
      def unwrap: String = i

    type MetaDataTag = "FiscalYear" | "AsOfDate"
    type FilingMode = "P" | "F"
    type ConsolidatedFlag = "CON" | "PAR" | "UNC"
    type CurrencyConversionModeId = "Historical" | "SpotRate"

  import Properties.*


  sealed trait Mnemonic
  object Mnemonic:
    // todo: reduce boilerplate of shared props with generic tuples: 
    // https://www.scala-lang.org/2021/02/26/tuples-bring-generic-programming-to-scala-3.html

    case class IQ_TOTAL_REV(properties: IQ_TOTAL_REV.Fn, identifier: Identifier) extends Mnemonic
    object IQ_TOTAL_REV:
      enum Fn:
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

      given Encoder[Fn] = Encoder.instance[Fn] {
          case fn: Fn.GDSP => 
            Json.obj(
              "currencyId" -> Json.fromString(fn.currencyId),
              "periodType" -> Json.fromString(fn.periodType.unwrap),
              "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
              "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
              "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
              "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
              "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
            )
          case fn: Fn.GDSHE =>
            Json.obj(
              "currencyId" -> Json.fromString(fn.currencyId),
              "periodType" -> Json.fromString(fn.periodType.unwrap),
              "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
              "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
              "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
              "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
              "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
              "metaDataTag" -> fn.metaDataTag.map(Json.fromString).getOrElse(Json.Null),
            )
        }

      given Encoder[IQ_TOTAL_REV] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSHE => "GDSHE" case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_TOTAL_REV",
            m.properties 
          )
      }

    end IQ_TOTAL_REV

    case class IQ_COMPANY_NAME_LONG(identifier: Identifier) extends Mnemonic
    object IQ_COMPANY_NAME_LONG:
      given Encoder[IQ_COMPANY_NAME_LONG] = 
        Encoder.forProduct3("function", "identifier", "mnemonic") { m => 
          ( "GDSP", m.identifier.unwrap, "IQ_COMPANY_NAME_LONG" )
        }

    end IQ_COMPANY_NAME_LONG

    // there is a StackOverflowError after using just a single `(_.asJson)`
    given Encoder[Mnemonic] = Encoder.instance[Mnemonic] {
      case m: Mnemonic.IQ_TOTAL_REV => m.asJson
      case m: Mnemonic.IQ_COMPANY_NAME_LONG => m.asJson
    }

  end Mnemonic

  case class Request(inputRequests: Vector[Mnemonic])
  object Request:
    given Encoder[Request] = Encoder.forProduct1("inputRequests")(_.inputRequests)

  case class RawResponse(responses: Vector[RawResponse.MnemonicResponse])
  object RawResponse:
    type Rows = Vector[Vector[String]] // the length of inner Vector is known before the request

    case class MnemonicResponse(error: String, rows: Option[Rows])
    object MnemonicResponse:
      given Decoder[RawResponse.MnemonicResponse] = Decoder.instance[RawResponse.MnemonicResponse] { cursor =>
        for 
          error <- cursor.get[String]("ErrMsg")
          jsonRows <- cursor.get[Option[Vector[Json]]]("Rows")
          rows <- jsonRows.traverse(_.traverse(_.hcursor.get[Vector[String]]("Row")))
        yield MnemonicResponse(error, rows)
      }

    given Decoder[RawResponse] = Decoder.instance[RawResponse](
      _.get[Vector[RawResponse.MnemonicResponse]]("GDSSDKResponse").map(RawResponse(_))
    )

end CapitalIQ