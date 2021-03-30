package com.quantemplate.capitaliq.domain

import java.time.*
import java.time.format.DateTimeFormatter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem
import cats.syntax.option.*

import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Properties.*
import com.quantemplate.capitaliq.domain.CapitalIQ.RawResponse.*

class RevenueReport(capitalIqService: CapitalIQService)(using system: ActorSystem[_]):
  given ExecutionContext = system.executionContext
  lazy val logger = LoggerFactory.getLogger(getClass)

  type ReportRows = Vector[Vector[Option[String]]]

  def generateSpreadSheet(ids: Vector[Identifier], range: (LocalDate, LocalDate)) = 
    for
      names <- getNameRows(ids)
      _ = logger.info("Fetched the company names")

      data <- getDataRows(ids, range)
      _ = logger.info("Fetched the report data")

      _ = viewAsXlsx(names, data)
      _ = logger.info("Constructed the spreadsheet")

    yield ()

  private def viewAsXlsx(names: ReportRows, data: ReportRows) = 
    val rows = names.zipWithIndex.map { case (row, i) => row ++ data(i) }
    val sheetModel = Vector(View.SheetModel("Revenue", rows))

    View.xlsx("CapitalIQ", sheetModel)

  private def getDataRows(
    ids: Vector[Identifier], 
    range: (LocalDate, LocalDate)
  ): Future[ReportRows] =
    import range.{ _1 => start, _2 => end }

    val periodType = "IQ_FY" back (end.getYear - start.getYear - 1)
    val asOfDate =  end.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")).some

    getReportData(periodType, asOfDate)(ids).map { responses => 
      val headerRange = (start.getYear to end.getYear).map(_.toString).toVector

      val headerRows = headerRange.map(_.some)
      val rows = responses.map { res =>
        headerRange.map { col => 
          res.rows.find(_.lift(1) == col.some).flatMap(_.headOption)
        }
      }

      headerRows +: rows
    }

  private def getReportData(periodType: MarkedPeriod, asOfDate: Option[String] = None) =
    capitalIqService.sendConcurrentRequests(
      ids => Request(
        ids.map { id =>  
          Mnemonic.IQ_TOTAL_REV(
            identifier = id,
            properties = Mnemonic.IQ_TOTAL_REV.Fn.GDSHE(
              currencyId = "USD",
              periodType = periodType,
              asOfDate = asOfDate,
              metaDataTag = "FiscalYear".some
            )
          )
        }
      )
    )

  private def getNameRows(ids: Vector[Identifier]): Future[ReportRows] =
    getReportNamesData(ids).map { responses =>
      val headerRows = Vector("Company name".some, "ID".some)
      val rows = responses.map { res =>
        Vector(
          res.rows.headOption.flatMap(_.headOption),
          res.mnemonic.asInstanceOf[Mnemonic.IQ_COMPANY_NAME_LONG].identifier.unwrap.some
        )
      }

      headerRows +: rows
    }

  private val getReportNamesData =
    capitalIqService.sendConcurrentRequests(
      ids => Request(ids.map(Mnemonic.IQ_COMPANY_NAME_LONG(_)))
    )
