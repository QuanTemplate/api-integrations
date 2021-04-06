package com.quantemplate.capitaliq.domain

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import org.slf4j.{LoggerFactory, Logger}
import akka.actor.typed.ActorSystem
import cats.syntax.option.*

import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Properties.*
import com.quantemplate.capitaliq.{View, Xlsx}

import com.quantemplate.capitaliq.qt.QTService

class RevenueReport(capitalIqService: CapitalIQService, qtService: QTService)(using system: ActorSystem[_]):
  given ExecutionContext = system.executionContext
  given logger: Logger = LoggerFactory.getLogger(getClass)

  type ReportRows = Vector[Vector[Option[String]]]

  def generateSpreadSheet(
    ids: Vector[Identifier], 
    range: (LocalDate, LocalDate),
    currency: String,
    orgId: String,
    datasetId: String
  ) = 
   measure {
      for
        names <- getNameRows(ids)
        _ = logger.info("Fetched the company names")

        data <- getDataRows(ids, range, currency)
        _ = logger.info("Fetched the report data")

        sheet = constructSpreadsheet(names, data)
        _ = logger.info("Constructed the spreadsheet")

        _ <- qtService.uploadDataset(sheet, orgId, datasetId)
        _ = logger.info("Uploaded the spreadsheet")
      yield ()
   }.onComplete { 
     case Failure(e) => 
      logger.error("Uncaught exception while generating the spreadsheet: {}", e.toString) 
     case Success(_) => ()
  }

  private def constructSpreadsheet(names: ReportRows, data: ReportRows): View =
    val rows = names.zipWithIndex.map { case (row, i) => row ++ data(i) }
    Xlsx(Vector(View.SheetModel("Revenue", rows))) 

  private def getDataRows(
    ids: Vector[Identifier], 
    range: (LocalDate, LocalDate),
    currency: String
  ): Future[ReportRows] =
    import range.{ _1 => start, _2 => end }

    val periodType = "IQ_FY" back (end.getYear - start.getYear - 1)
    val asOfDate =  end.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")).some

    getReportData(periodType, currency, asOfDate)(ids).map { responses => 
      val headerRange = (start.getYear to end.getYear).map(_.toString).toVector

      val headerRows = headerRange.map(_.some)
      val rows = responses.map { res =>
        headerRange.map { col => 
          res.rows.find(_.lift(1) == col.some).flatMap(_.headOption)
        }
      }

      headerRows +: rows
    }

  private def getReportData(
    periodType: MarkedPeriod,
    currency: String, 
    asOfDate: Option[String],
  ) =
    capitalIqService.sendConcurrentRequests(
      ids => Request(
        ids.map { id =>  
          Mnemonic.IQ_TOTAL_REV(
            identifier = id,
            properties = Mnemonic.IQ_TOTAL_REV.Fn.GDSHE(
              currencyId = currency,
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


def measure[T](codeBlock: => Future[T])
(using logger: Logger, ec: ExecutionContext): Future[T] =
  val t0 = System.nanoTime()
  codeBlock.map { result => 
    val t1 = System.nanoTime()

    val elapsed = TimeUnit.SECONDS.convert(t1 - t0, TimeUnit.NANOSECONDS)
    logger.info("Finished in: " + elapsed + "s")

    result
  }