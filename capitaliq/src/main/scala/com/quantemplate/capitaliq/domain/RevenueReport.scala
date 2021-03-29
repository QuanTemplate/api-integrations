package com.quantemplate.capitaliq.domain

import java.time.*
import java.time.format.DateTimeFormatter

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem

import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Properties.*
import com.quantemplate.capitaliq.domain.CapitalIQ.RawResponse.*

class RevenueReport(capitalIqService: CapitalIQService)(using system: ActorSystem[_]):
  given ExecutionContext = system.executionContext
  lazy val logger = LoggerFactory.getLogger(getClass)

  def generate(
    ids: Vector[Identifier],
    range: (LocalDate, LocalDate),
  ) = 
    import range.{ _1 => start, _2 => end }

    val periodType = "IQ_FY" back (end.getYear - start.getYear - 1)
    val asOfDate =  Some(end.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))

    for 
      response <- getReportData(periodType, asOfDate)(ids)
      _ = logger.info("Got the RevenueReport response")
      rows = constructRows(response, range)
      _ = logger.info("Constructed the rows")

    yield View.xlsx(
      "CapitalIQ", 
      Vector(View.SheetModel("Revenue", rows))
    )

  private def constructRows(
    res: Vector[CapitalIQService.Response],
    range: (LocalDate, LocalDate)): Vector[Vector[Option[String]]] = 
    import range.{ _1 => start, _2 => end }

    val headerRange = (start.getYear to end.getYear).map(_.toString).toVector

    val headerRows = headerRange.map(Some(_))
    val rows = res.map { r =>
      headerRange.map(c => r.rows.find(_._2 == c).map(_._1))
    }

    headerRows +: rows

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
              metaDataTag = Some("FiscalYear")
            )
          )
        }
      )
    )
