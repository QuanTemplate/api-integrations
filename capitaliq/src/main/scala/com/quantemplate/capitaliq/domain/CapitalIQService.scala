package com.quantemplate.capitaliq.domain

import scala.concurrent.ExecutionContext
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import akka.util.ByteString

import com.quantemplate.capitaliq.{Config, HttpService}
import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Properties.*

class CapitalIQService(httpService: HttpService)(using system: ActorSystem[_], conf: Config):
  given ExecutionContext = system.executionContext
  lazy val logger = LoggerFactory.getLogger(getClass)

  def getRevenueReport(ids: Seq[Identifier]) = 
    // TODO: refactor date range calculation
    val req = Request(
        ids.map { id =>  
          Mnemonic.IQ_TOTAL_REV(
            properties = Mnemonic.IQ_TOTAL_REV.Fn.GDSHE(
                currencyId = "USD",
                // from 1988 
                // (`asOfDate`.year - 1988 - 1 = 29) 🤯
                periodType = "IQ_FY" back 29,
                // to 2018
                asOfDate = Some("12/31/2018"),
                metaDataTag = Some("FiscalYear")
            ),
            identifier = id
          )
        }
      )

    sendRequest(req)
      .map { result => 
        logger.info("Got response: " + result) 
      }
      .recover { 
        //  ERROR com.quantemplate.capitaliq.domain.CapitalIQService - Request error: DecodingFailure(C[A], List(DownArray, DownField(GDSSDKResponse)))
        // :thinking
        case e: Throwable => logger.error("Request error: " + e)
      }

  private def sendRequest(req: Request) =
    httpService.POST[Request, Response]( 
      conf.capitaliq.endpoint,
      req, 
      Some(
        Authorization(
          BasicHttpCredentials(
            conf.capitaliq.credentials.username, 
            conf.capitaliq.credentials.password
          )
        )
      )
    )
      
  
  