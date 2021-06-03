package com.quantemplate.integrations.capitaliq

import io.circe.{ Encoder, Decoder, Json }
import io.circe.syntax.given
import cats.syntax.traverse.given
import cats.syntax.applicative.given

/**
 *  CapitalIQ exposes.capitaliq models, which are direct, type-safe mapping of Capital IQ API
 */
object CapitalIQ:
  opaque type Identifier = String
  object Identifier:
    val prefix = "IQ"

    def apply(s: String): Identifier = s

    // this might use some improved validation
    def isValid(s: String) = s.startsWith(prefix)

    given Decoder[Identifier] = Decoder.decodeString
      .ensure(isValid, "Not a valid CapitalIQ identifier")
      .map(Identifier(_))

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


  sealed trait Mnemonic:
    def name: String
  object Mnemonic:
    // If this is going to be maintained in a long term, the boilerplate needs to be reduced, mainly by:
    //  - switching to Scala 3 `derives` mechanism and somehow configuring the ADT discriminant: https://github.com/circe/circe/issues/1777
    //  - describing shared props with generic tuples 
    //    https://www.scala-lang.org/2021/02/26/tuples-bring-generic-programming-to-scala-3.html
    //    beware that some of the mnemonic properties could include subtle differences (!)
    //    before any sudden refactor consult the api docs: https://support.standardandpoors.com/gds/ first
    //  - getting rid of `name` method, or implementing it one time in the sealed trait
    //    for some reason the the `java.lang.InternalError: Malformed class name` is thrown when we try to use .getClass.getSimpleName
    //    could be a Scala 3 bug

    case class IQ_TOTAL_REV(properties: IQ_TOTAL_REV.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_TOTAL_REV:
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )

        case GDSHE(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          metaDataTag: Option[MetaDataTag] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )

      given Encoder[Fn] = Encoder {
          case fn: Fn.GDSP => 
            Json.obj(
              "currencyId" -> Json.fromString(fn.currencyId),
              "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
              "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
              "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
              "filingMode" ->  fn.filingMode.map(Json.fromString).getOrElse(Json.Null),
              "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
              "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
            )
          case fn: Fn.GDSHE =>
            Json.obj(
              "currencyId" -> Json.fromString(fn.currencyId),
              "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
              "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
              "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
              "filingMode" ->  fn.filingMode.map(Json.fromString).getOrElse(Json.Null),
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

    // last total TEV - Total Enterprise Value
    case class IQ_TEV_TOTAL_REV(properties: IQ_TEV_TOTAL_REV.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_TEV_TOTAL_REV:
      // also supports GDST and GDSHE
      enum Fn:
        case GDSP(
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_TEV_TOTAL_REV] = 
        Encoder.forProduct3("function", "identifier", "mnemonic") { m => 
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" }, 
            m.identifier.unwrap, 
            "IQ_TEV_TOTAL_REV" 
          )
        }
    end IQ_TEV_TOTAL_REV



    case class IQ_COMPANY_NAME_LONG(identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_COMPANY_NAME_LONG:
      given Encoder[IQ_COMPANY_NAME_LONG] = 
        Encoder.forProduct3("function", "identifier", "mnemonic") { m => 
          ( "GDSP", m.identifier.unwrap, "IQ_COMPANY_NAME_LONG" )
        }

    case class IQ_ULT_PARENT(identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_ULT_PARENT:
      given Encoder[IQ_ULT_PARENT] = 
        Encoder.forProduct3("function", "identifier", "mnemonic") { m => 
          ( "GDSP", m.identifier.unwrap, "IQ_ULT_PARENT" )
        }

    case class IQ_ULT_PARENT_CIQID(identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_ULT_PARENT_CIQID:
      given Encoder[IQ_ULT_PARENT_CIQID] = 
        Encoder.forProduct3("function", "identifier", "mnemonic") { m => 
          ( "GDSP", m.identifier.unwrap, "IQ_ULT_PARENT_CIQID" )
        }

    case class IQ_COMPANY_ID(properties: IQ_COMPANY_ID.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_COMPANY_ID:
      enum Fn:
        case GDSP(startDate: Option[String] = None)

      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "startDate" -> fn.startDate.map(Json.fromString).getOrElse(Json.Null)
          )
      }

      given Encoder[IQ_COMPANY_ID] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m => 
          ("GDSP", m.identifier.unwrap, "IQ_COMPANY_ID", m.properties )
        }
    end IQ_COMPANY_ID

    case class IQ_MARKETCAP(properties: IQ_MARKETCAP.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_MARKETCAP:
      // also supports GDST and GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          asOfDate: Option[String] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )

      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_MARKETCAP] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_MARKETCAP",
            m.properties 
          )
        }
    end IQ_MARKETCAP

    // net income
    case class IQ_NI(properties: IQ_NI.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_NI:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_NI] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_NI",
            m.properties 
          )
        }
    end IQ_NI

    case class IQ_TOTAL_EMPLOYEES(properties: IQ_TOTAL_EMPLOYEES.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_TOTAL_EMPLOYEES:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_TOTAL_EMPLOYEES] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_TOTAL_EMPLOYEES",
            m.properties 
          )
        }
    end IQ_TOTAL_EMPLOYEES

    case class IQ_EBIT(properties: IQ_EBIT.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_EBIT:
        // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP =>
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_EBIT] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_EBIT",
            m.properties 
          )
        }

    end IQ_EBIT

    case class IQ_TEV_EBIT(properties: IQ_TEV_EBIT.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_TEV_EBIT:
      // also supports GDST and GDSHE
      enum Fn:
        case GDSP(
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_TEV_EBIT] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_TEV_EBIT",
            m.properties 
          )
        }
    end IQ_TEV_EBIT

    // EBITDA - Earnings Before Interest, Taxes, Depreciation, and Amortization
    case class IQ_EBITDA(properties: IQ_EBITDA.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_EBITDA:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_EBITDA] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_EBITDA",
            m.properties 
          )
        }
    end IQ_EBITDA

    case class IQ_TEV_EBITDA(properties: IQ_TEV_EBITDA.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_TEV_EBITDA:
      // also supports GDST and GDSHE
      enum Fn:
        case GDSP(
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_TEV_EBITDA] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_TEV_EBITDA",
            m.properties 
          )
        }
    end IQ_TEV_EBITDA

    case class IQ_DILUT_EPS_EXCL(properties: IQ_DILUT_EPS_EXCL.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_DILUT_EPS_EXCL:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_DILUT_EPS_EXCL] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_DILUT_EPS_EXCL",
            m.properties 
          )
        }
    end IQ_DILUT_EPS_EXCL

    case class IQ_PE_EXCL(properties: IQ_PE_EXCL.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_PE_EXCL:
      // also supports GDST and GDSHE
      enum Fn:
        case GDSP(
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_PE_EXCL] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_PE_EXCL",
            m.properties 
          )
        }
    end IQ_PE_EXCL

    case class IQ_TOTAL_REV_1YR_ANN_GROWTH(properties: IQ_TOTAL_REV_1YR_ANN_GROWTH.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_TOTAL_REV_1YR_ANN_GROWTH:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_TOTAL_REV_1YR_ANN_GROWTH] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_TOTAL_REV_1YR_ANN_GROWTH",
            m.properties 
          )
        }
    end IQ_TOTAL_REV_1YR_ANN_GROWTH

    case class IQ_EBITDA_1YR_ANN_GROWTH(properties: IQ_EBITDA_1YR_ANN_GROWTH.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_EBITDA_1YR_ANN_GROWTH:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_EBITDA_1YR_ANN_GROWTH] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_EBITDA_1YR_ANN_GROWTH",
            m.properties 
          )
        }
    end IQ_EBITDA_1YR_ANN_GROWTH

    case class IQ_EBIT_1YR_ANN_GROWTH(properties: IQ_EBIT_1YR_ANN_GROWTH.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_EBIT_1YR_ANN_GROWTH:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_EBIT_1YR_ANN_GROWTH] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_EBIT_1YR_ANN_GROWTH",
            m.properties 
          )
        }
    end IQ_EBIT_1YR_ANN_GROWTH

    case class IQ_NI_1YR_ANN_GROWTH(properties: IQ_NI_1YR_ANN_GROWTH.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_NI_1YR_ANN_GROWTH:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_NI_1YR_ANN_GROWTH] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_NI_1YR_ANN_GROWTH",
            m.properties 
          )
        }
    end IQ_NI_1YR_ANN_GROWTH

    case class IQ_EPS_1YR_ANN_GROWTH(properties: IQ_EPS_1YR_ANN_GROWTH.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_EPS_1YR_ANN_GROWTH:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_EPS_1YR_ANN_GROWTH] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_EPS_1YR_ANN_GROWTH",
            m.properties 
          )
        }
    end IQ_EPS_1YR_ANN_GROWTH

    case class IQ_TOTAL_ASSETS(properties: IQ_TOTAL_ASSETS.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_TOTAL_ASSETS:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_TOTAL_ASSETS] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_TOTAL_ASSETS",
            m.properties 
          )
        }
    end IQ_TOTAL_ASSETS

    case class IQ_RETURN_ASSETS(properties: IQ_RETURN_ASSETS.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_RETURN_ASSETS:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_RETURN_ASSETS] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_RETURN_ASSETS",
            m.properties 
          )
        }
    end IQ_RETURN_ASSETS

    case class IQ_RETURN_CAPITAL(properties: IQ_RETURN_CAPITAL.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_RETURN_CAPITAL:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_RETURN_CAPITAL] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_RETURN_CAPITAL",
            m.properties 
          )
        }
    end IQ_RETURN_CAPITAL

    case class IQ_RETURN_EQUITY(properties: IQ_RETURN_EQUITY.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_RETURN_EQUITY:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_RETURN_EQUITY] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_RETURN_EQUITY",
            m.properties 
          )
        }
    end IQ_RETURN_EQUITY

    case class IQ_RETURN_COMMON_EQUITY(properties: IQ_RETURN_COMMON_EQUITY.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_RETURN_COMMON_EQUITY:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_RETURN_COMMON_EQUITY] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_RETURN_COMMON_EQUITY",
            m.properties 
          )
        }
    end IQ_RETURN_COMMON_EQUITY

    case class IQ_GROSS_MARGIN(properties: IQ_GROSS_MARGIN.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_GROSS_MARGIN:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_GROSS_MARGIN] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_GROSS_MARGIN",
            m.properties 
          )
        }
    end IQ_GROSS_MARGIN

    case class IQ_EBITDA_MARGIN(properties: IQ_EBITDA_MARGIN.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_EBITDA_MARGIN:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_EBITDA_MARGIN] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_EBITDA_MARGIN",
            m.properties 
          )
        }
    end IQ_EBITDA_MARGIN

    case class IQ_EBIT_MARGIN(properties: IQ_EBIT_MARGIN.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_EBIT_MARGIN:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_EBIT_MARGIN] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_EBIT_MARGIN",
            m.properties 
          )
        }
    end IQ_EBIT_MARGIN

    case class IQ_NI_MARGIN(properties: IQ_NI_MARGIN.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_NI_MARGIN:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_NI_MARGIN] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_NI_MARGIN",
            m.properties 
          )
        }
    end IQ_NI_MARGIN

    case class IQ_CAPEX_PCT_REV(properties: IQ_CAPEX_PCT_REV.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_CAPEX_PCT_REV:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_CAPEX_PCT_REV] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_CAPEX_PCT_REV",
            m.properties 
          )
        }
    end IQ_CAPEX_PCT_REV

    case class IQ_TOTAL_DEBT_EBITDA(properties: IQ_TOTAL_DEBT_EBITDA.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_TOTAL_DEBT_EBITDA:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_TOTAL_DEBT_EBITDA] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_TOTAL_DEBT_EBITDA",
            m.properties 
          )
        }
    end IQ_TOTAL_DEBT_EBITDA

    case class IQ_TOTAL_DEBT_EQUITY(properties: IQ_TOTAL_DEBT_EQUITY.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_TOTAL_DEBT_EQUITY:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_TOTAL_DEBT_EQUITY] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_TOTAL_DEBT_EQUITY",
            m.properties 
          )
        }
    end IQ_TOTAL_DEBT_EQUITY

    case class IQ_NET_DEBT_EBITDA(properties: IQ_NET_DEBT_EBITDA.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_NET_DEBT_EBITDA:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_NET_DEBT_EBITDA] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_NET_DEBT_EBITDA",
            m.properties 
          )
        }
    end IQ_NET_DEBT_EBITDA

    case class IQ_NET_DEBT_EBITDA_CAPEX(properties: IQ_NET_DEBT_EBITDA_CAPEX.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_NET_DEBT_EBITDA_CAPEX:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_NET_DEBT_EBITDA_CAPEX] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_NET_DEBT_EBITDA_CAPEX",
            m.properties 
          )
        }
    end IQ_NET_DEBT_EBITDA_CAPEX

    case class IQ_CASH_INTEREST(properties: IQ_CASH_INTEREST.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_CASH_INTEREST:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_CASH_INTEREST] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_CASH_INTEREST",
            m.properties 
          )
        }
    end IQ_CASH_INTEREST

    case class IQ_NET_DEBT(properties: IQ_NET_DEBT.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_NET_DEBT:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_NET_DEBT] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_NET_DEBT",
            m.properties 
          )
        }
    end IQ_NET_DEBT

    case class IQ_NET_INTEREST_EXP(properties: IQ_NET_INTEREST_EXP.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_NET_INTEREST_EXP:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_NET_INTEREST_EXP] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_NET_INTEREST_EXP",
            m.properties 
          )
        }
    end IQ_NET_INTEREST_EXP

    case class IQ_EBITDA_CAPEX(properties: IQ_EBITDA_CAPEX.Fn, identifier: Identifier) extends Mnemonic:
      def name = productPrefix
    object IQ_EBITDA_CAPEX:
      // also supports GDSHE
      enum Fn:
        case GDSP(
          currencyId: String,
          periodType: Option[MarkedPeriod] = None,
          asOfDate: Option[String] = None,
          restatementTypeId: Option[String] = None,
          filingMode: Option[FilingMode] = None,
          consolidatedFlag: Option[ConsolidatedFlag] = None,
          currencyConversionModeId: Option[CurrencyConversionModeId] = None
        )
      
      given Encoder[Fn] = Encoder {
        case fn: Fn.GDSP => 
          Json.obj(
            "currencyId" -> Json.fromString(fn.currencyId),
            "periodType" -> fn.periodType.map(_.unwrap).map(Json.fromString).getOrElse(Json.Null),
            "asOfDate" -> fn.asOfDate.map(Json.fromString).getOrElse(Json.Null),
            "restatementTypeId" -> fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "filingMode" ->  fn.restatementTypeId.map(Json.fromString).getOrElse(Json.Null),
            "consolidatedFlag" -> fn.consolidatedFlag.map(Json.fromString).getOrElse(Json.Null),
            "currencyConversionModeId" -> fn.currencyConversionModeId.map(Json.fromString).getOrElse(Json.Null),
          )
      }

      given Encoder[IQ_EBITDA_CAPEX] = 
        Encoder.forProduct4("function", "identifier", "mnemonic", "properties") { m =>
          (
            m.properties match { case _: Fn.GDSP  => "GDSP" },
            m.identifier.unwrap,
            "IQ_EBITDA_CAPEX",
            m.properties 
          )
        }
    end IQ_EBITDA_CAPEX

    // there is a StackOverflowError after using just a single `(_.asJson)`
    given Encoder[Mnemonic] = Encoder.instance[Mnemonic] {
      case m: IQ_TOTAL_REV => m.asJson
      case m: IQ_TEV_TOTAL_REV => m.asJson
      case m: IQ_COMPANY_NAME_LONG => m.asJson
      case m: IQ_COMPANY_ID => m.asJson
      case m: IQ_ULT_PARENT => m.asJson
      case m: IQ_ULT_PARENT_CIQID => m.asJson
      case m: IQ_MARKETCAP => m.asJson
      case m: IQ_NI => m.asJson
      case m: IQ_TOTAL_EMPLOYEES => m.asJson
      case m: IQ_EBIT => m.asJson
      case m: IQ_TEV_EBIT => m.asJson
      case m: IQ_EBITDA => m.asJson
      case m: IQ_TEV_EBITDA => m.asJson
      case m: IQ_DILUT_EPS_EXCL => m.asJson
      case m: IQ_PE_EXCL => m.asJson
      case m: IQ_TOTAL_REV_1YR_ANN_GROWTH => m.asJson
      case m: IQ_EBITDA_1YR_ANN_GROWTH => m.asJson
      case m: IQ_EBIT_1YR_ANN_GROWTH => m.asJson
      case m: IQ_NI_1YR_ANN_GROWTH => m.asJson
      case m: IQ_EPS_1YR_ANN_GROWTH => m.asJson
      case m: IQ_TOTAL_ASSETS => m.asJson
      case m: IQ_RETURN_ASSETS => m.asJson
      case m: IQ_RETURN_CAPITAL => m.asJson
      case m: IQ_RETURN_EQUITY => m.asJson
      case m: IQ_RETURN_COMMON_EQUITY => m.asJson
      case m: IQ_GROSS_MARGIN => m.asJson
      case m: IQ_EBITDA_MARGIN => m.asJson
      case m: IQ_EBIT_MARGIN => m.asJson
      case m: IQ_NI_MARGIN => m.asJson
      case m: IQ_CAPEX_PCT_REV => m.asJson
      case m: IQ_TOTAL_DEBT_EBITDA => m.asJson
      case m: IQ_TOTAL_DEBT_EQUITY => m.asJson 
      case m: IQ_NET_DEBT_EBITDA => m.asJson
      case m: IQ_NET_DEBT_EBITDA_CAPEX => m.asJson
      case m: IQ_CASH_INTEREST => m.asJson
      case m: IQ_NET_DEBT => m.asJson
      case m: IQ_NET_INTEREST_EXP => m.asJson
      case m: IQ_EBITDA_CAPEX => m.asJson
    }

  end Mnemonic

  case class Request(inputRequests: Vector[Mnemonic]) derives Encoder.AsObject

  case class RawResponse(responses: Vector[RawResponse.MnemonicResponse])
  object RawResponse:
    type Rows = Vector[Vector[String]] // the length of inner Vector is known before the request

    case class MnemonicResponse(error: String, rows: Option[Rows])
    object MnemonicResponse:
      given Decoder[RawResponse.MnemonicResponse] = Decoder { c =>
        for 
          error <- c.get[String]("ErrMsg")
          jsonRows <- c.get[Option[Vector[Json]]]("Rows")
          rows <- jsonRows.traverse(_.traverse(_.hcursor.get[Vector[String]]("Row")))
        yield MnemonicResponse(error, rows)
      }

    given Decoder[RawResponse] = Decoder(
      _.get[Vector[RawResponse.MnemonicResponse]]("GDSSDKResponse").map(RawResponse(_))
    )

end CapitalIQ
