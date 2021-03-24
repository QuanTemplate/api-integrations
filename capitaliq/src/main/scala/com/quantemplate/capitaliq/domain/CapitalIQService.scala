package com.quantemplate.capitaliq

import scala.concurrent.ExecutionContext
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*
import org.slf4j.LoggerFactory
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.marshalling.Marshal
import akka.actor.typed.ActorSystem
import akka.util.ByteString

import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Properties.*

class CapitalIQService(httpService: HttpService)(using system: ActorSystem[_]):
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
      res <- httpService.sendRequest(req)
    yield 
      res.entity.dataBytes.runFold(ByteString(""))(_ ++ _) foreach { body =>
        logger.info("Got response, body: " + body.utf8String)
      }
  
  