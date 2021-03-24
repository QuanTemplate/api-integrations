package com.quantemplate.capitaliq

import scala.concurrent.ExecutionContext
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import akka.util.ByteString

import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Properties.*

class CapitalIQService(httpService: HttpService)(using system: ActorSystem[_], conf: Config):
  given ExecutionContext = system.executionContext
  lazy val logger = LoggerFactory.getLogger(getClass)

  def getRevenueReport() = 

    val req = Request(
        Seq(
          Mnemonic.IQ_TOTAL_REV(
            properties = Mnemonic.IQ_TOTAL_REV.Fn.GDSHE(
                currencyId = "USD",
                periodType = "IQ_FY" back 31,
                metaDataTag = Some("FiscalYear")
            ),
            identifier = Identifier("IQ121238")
          )
        )
      )

    
    for
      res <- sendRequest(req)
    yield 
      res.entity.dataBytes.runFold(ByteString(""))(_ ++ _) foreach { body =>
        logger.info("Got response, body: " + body.utf8String)
      }

  private def sendRequest(req: Request) =
    httpService.POST( 
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
      
  
  