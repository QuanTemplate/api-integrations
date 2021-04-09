package com.quantemplate.capitaliq.domain

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import cats.syntax.option.*

import com.quantemplate.capitaliq.common.{Config, HttpService}
import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.RawResponse.*

class CapitalIQService(httpService: HttpService)(using system: ActorSystem[_], conf: Config):
  import CapitalIQService.*

  given ExecutionContext = system.executionContext
  lazy val logger = system.log

  def sendConcurrentRequests(toReq: Vector[Identifier] => Request)(ids: Vector[Identifier]) = 
    val requests = ids
      .grouped(conf.capitaliq.mnemonicsPerRequest)
      .toVector
      .map(toReq.andThen(sendRequest))

    Future
      .sequence(requests)
      .map(_.flatten)

  def sendRequest(req: Request)(using ExecutionContext): Future[Vector[Response]] =
    httpService.post[Request, RawResponse](
      conf.capitaliq.endpoint,
      req, 
      Authorization(
        BasicHttpCredentials(
          conf.capitaliq.credentials.username, 
          conf.capitaliq.credentials.password
        )
      ).some
    ).map(adaptRawResponse(req))

  private def adaptRawResponse(req: Request)(rawRes: RawResponse) =
    val res = rawRes.responses

    val generalErrors = collectGeneralErrors(res)
    if !generalErrors.isEmpty then throw MnemonicsError(generalErrors)

    val result = req
      .inputRequests
      .zipWithIndex
      .toVector
      .map { case (req, i) => (req, res(i)) }

    val perMnemonicErrors = collectPerMnemonicErrors(result)
    if !perMnemonicErrors.isEmpty then throw MnemonicsError(perMnemonicErrors)
    
    result.map { case (req, res) => 
      Response(
        req, 
        res.rows.get.filter(data => !(data.lift(0) == "Data Unavailable".some))
      ) 
    }

  private def collectGeneralErrors(result: Vector[MnemonicResponse]) =
    result.collect {
      case r @ MnemonicResponse("Daily Request Limit of 24000 Exceeded", _) =>
        logger.error("DailyMnemonicLimitReachedError: {}", r.error) 
        DailyMnemonicLimitReachedError
    }

  private def collectPerMnemonicErrors(result: Vector[(Mnemonic, MnemonicResponse)]) = 
    result.collect {
      case r @ (_, MnemonicResponse("InvalidTimePeriod", _)) =>
        logger.error("InvalidServiceParametersError: {}", r._2.error) 
        InvalidServiceParametersError(r._2.error)

      case r @ (mnemonic, MnemonicResponse("InvalidIdentifier", _)) =>
        logger.error("InvalidServiceParametersError: {} in {}", r._2.error, mnemonic.toString) 
        InvalidServiceParametersError(r._2.error)

      case (_, MnemonicResponse(error, _)) if !error.isEmpty =>
        logger.error("UnrecognizedServiceError: {}", error)
        UnrecognizedServiceError(error)

      case (_, MnemonicResponse(_, rows)) if rows.isEmpty => 
        logger.error("UnexpectedEmptyRows")
        UnexpectedEmptyRows("Mnemonic not mapped")
    } 
object CapitalIQService:
  case class Response(mnemonic: Mnemonic, rows: RawResponse.Rows)
  