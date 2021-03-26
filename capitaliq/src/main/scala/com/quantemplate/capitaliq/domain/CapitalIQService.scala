package com.quantemplate.capitaliq.domain

import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import akka.util.ByteString

import com.quantemplate.capitaliq.{Config, HttpService}
import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Properties.*
import com.quantemplate.capitaliq.domain.CapitalIQ.RawResponse.*

class CapitalIQService(httpService: HttpService)(using system: ActorSystem[_], conf: Config):
  import CapitalIQService.*

  given ExecutionContext = system.executionContext
  lazy val logger = LoggerFactory.getLogger(getClass)

  val getRevenueReport = 
    sendConcurrentRequests(
      ids => Request(
        ids.map { id =>  
          Mnemonic.IQ_TOTAL_REV(
            properties = Mnemonic.IQ_TOTAL_REV.Fn.GDSHE(
                currencyId = "USD",
                // data from 1988 - 2018
                // (`asOfDate`.year - 1988 - 1 = 29) 🤯
                periodType = "IQ_FY" back 29,
                asOfDate = Some("12/31/2018"),
                metaDataTag = Some("FiscalYear")
            ),
            identifier = id
          )
        }
      )
    )

  private def sendConcurrentRequests(toReq: Vector[Identifier] => Request)(ids: Vector[Identifier]) = 
    val requests = ids
      .grouped(conf.capitaliq.mnemonicsPerRequest)
      .toVector
      .map(toReq.andThen(sendRequest))

    Future
      .sequence(requests)
      .map(_.flatten)
      .map { result => 
        logger.info("Got response: " + result)
      
      }
      .recover { 
        case e: Throwable => logger.error("Request error: " + e)
      }
      

  private def sendRequest(req: Request)(using ExecutionContext): Future[Vector[Response]] =
    httpService.POST[Request, RawResponse](
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
    ) map { 
        case RawResponse(res) => 
          val errors = res.collect {
            case r @ MnemonicResponse("InvalidTimePeriod", _, _) => InvalidServiceParametersError(r.error)
            case MnemonicResponse(error, _, _) if !error.isEmpty => UnrecognizedServiceError(error)
            case MnemonicResponse(_, mnemonic, rows) if rows.isEmpty => UnexpectedEmptyRows(mnemonic)
          }

          if !errors.isEmpty 
            then throw MnemonicsError(errors)
            else res.map(r => Response(r.mnemonic, r.rows.get))
    }
      
object CapitalIQService:
  case class Response(mnemonic: String, rows: RawResponse.Rows)