package com.quantemplate.capitaliq

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import akka.util.ByteString
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.RequestEntity
import akka.http.scaladsl.marshalling.Marshal

import io.circe.syntax.*

import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Properties.*
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.*


@main
def run() =
  given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  given ExecutionContext = system.executionContext

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
    entity <- Marshal(req).to[RequestEntity]
    res <- sendRequest(entity)
  yield (
    res.entity.dataBytes.runFold(ByteString(""))(_ ++ _) foreach { body =>
      system.log.info("Got response, body: " + body.utf8String)
      system.terminate()
    }
  )
  
def sendRequest(entity: RequestEntity)(using ActorSystem[_]) = 
    Http()
      .singleRequest(
       // TODO: take consts from config
        HttpRequest(
          method = HttpMethods.POST,
          // uri = "https://jsonplaceholder.typicode.com/posts",
          uri = "https://api-ciq.marketintelligence.spglobal.com/gdsapi/rest/v3/clientservice.json",
          entity = entity,
          headers = Seq(
            Authorization(
              BasicHttpCredentials("apiadmin@quantemplate.com", "---")
            )
          ),
        )
      )