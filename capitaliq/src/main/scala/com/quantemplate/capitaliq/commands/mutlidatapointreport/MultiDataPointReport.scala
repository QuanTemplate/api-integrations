package com.quantemplate.capitaliq.commands.mutlidatapointreport

import scala.concurrent.{ExecutionContext, Future}
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import org.slf4j.{LoggerFactory, Logger}
import cats.syntax.option.given
import cats.syntax.traverse.given

import com.quantemplate.capitaliq.common.*
import com.quantemplate.capitaliq.domain.CapitalIQService
import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Mnemonic.*
import com.quantemplate.capitaliq.qt.QTService

class MultiDataPointReport(capitalIqService: CapitalIQService, qtService: QTService)(using ExecutionContext):
  given logger: Logger = LoggerFactory.getLogger(getClass)

  def generateSpreadSheet(config: CmdConfig) =
    measure {
      for  
        data <- getDataRows(config)
        _ = logger.info("Fetched the report data")

        sheet = constructSpreadsheet(data)
         _ = logger.info("Constructed the spreadsheet")

        _ <- qtService.uploadDataset(sheet, config.orgId, config.datasetId)
        _ = logger.info("Uploaded the spreadsheet")
        
      yield ()
    }.recover { case e: Throwable => 
      println(e)
      e.printStackTrace
      e
    }

  private def constructSpreadsheet(data: View.ReportRows): View =
    Xlsx(Vector(View.SheetModel("MultiDataPoint", data)))

  private def getDataRows(config: CmdConfig): Future[View.ReportRows] = 
    constructMnemonicRequests(config)
      .map { fn => capitalIqService.sendConcurrentRequests(ids => Request(ids.map(fn))) }
      .map { fn => fn(config.identifiers) }
      .sequence
      .map { columnResponses => 
        val headerRow = columnResponses.map(_.headOption.map(_.mnemonic.name))
        // assume that only a single data point is returned per mnemonic (GDSP func)
        val columns = columnResponses.map(_.map(_.rows.headOption.flatMap(_.headOption)))

        headerRow +: columns.transpose
      }

  private def constructMnemonicRequests(config: CmdConfig): Vector[Identifier => Mnemonic] =
    val ids = config.identifiers
    val asOfDate = config.date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")).some
    val currencyId = config.currency

    config.columns.collect {
      case ColumnDef("IQ_TOTAL_REV", _) => 
        id => 
          IQ_TOTAL_REV(
            IQ_TOTAL_REV.Fn.GDSP(
              currencyId = currencyId,
              asOfDate = asOfDate
            ),
            identifier = id
          )

      case ColumnDef("IQ_COMPANY_NAME_LONG", _) => 
        IQ_COMPANY_NAME_LONG.apply

      case ColumnDef("IQ_ULT_PARENT", _) => 
        IQ_ULT_PARENT.apply

      case ColumnDef("IQ_COMPANY_ID", _) =>
        id => 
          IQ_COMPANY_ID(
            IQ_COMPANY_ID.Fn.GDSP(),
            identifier = id
          )

      case ColumnDef("IQ_MARKETCAP", _) =>
        id => 
          IQ_MARKETCAP(
            IQ_MARKETCAP.Fn.GDSP(
              currencyId = currencyId,
              asOfDate = asOfDate
            ),
            identifier = id
          )

      case ColumnDef("IQ_NI", _) =>
        id => 
          IQ_NI(
            IQ_NI.Fn.GDSP(
              currencyId = currencyId,
              asOfDate = asOfDate
            ),
            identifier = id
          )

      case ColumnDef("IQ_TOTAL_EMPLOYEES", _) =>
        id => 
          IQ_TOTAL_EMPLOYEES(
            IQ_TOTAL_EMPLOYEES.Fn.GDSP(
              currencyId = currencyId,
              asOfDate = asOfDate
            ),
            identifier = id
          )
        
      case ColumnDef("IQ_EBITDA", _) =>
        id => 
          IQ_EBITDA(
            IQ_EBITDA.Fn.GDSP(
              currencyId = currencyId,
              asOfDate = asOfDate
            ),
            identifier = id
          )
    }
