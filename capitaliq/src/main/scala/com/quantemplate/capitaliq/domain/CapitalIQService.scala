package com.quantemplate.capitaliq.domain

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.{ Authorization, BasicHttpCredentials }
import cats.syntax.option.given
import org.slf4j.LoggerFactory

import com.quantemplate.capitaliq.common.{Config, HttpService}
import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQ.RawResponse.*

class CapitalIQService(httpService: HttpService)(using ec: ExecutionContext, conf: Config):
  import CapitalIQService.*

  lazy val logger = LoggerFactory.getLogger(getClass)

  def sendConcurrentRequests(
    toReq: Vector[Identifier] => Request, 
    errorStrategy: ErrorStrategy = ErrorStrategy.DiscardOnlyInvalid
  )(ids: Vector[Identifier]) = 
    val requests = ids
      .grouped(conf.capitaliq.mnemonicsPerRequest)
      .toVector
      .map(toReq.andThen(sendRequest(_, errorStrategy)))

    Future
      .sequence(requests)
      .map(_.flatten)

  def sendRequest(
    req: Request, 
    errorStrategy: ErrorStrategy = ErrorStrategy.DiscardOnlyInvalid
  ): Future[Vector[Response]] =
    httpService.post[Request, RawResponse](
      conf.capitaliq.endpoint,
      req, 
      Authorization(
        BasicHttpCredentials(
          conf.capitaliq.credentials.username, 
          conf.capitaliq.credentials.password
        )
      ).some
    )
      .map(adaptRawResponse(req, errorStrategy))

  private def adaptRawResponse(req: Request, errorStrategy: ErrorStrategy)(rawRes: RawResponse) =
    val res = rawRes.responses

    val generalErrors = collectGeneralErrors(res)
    if !generalErrors.isEmpty then throw MnemonicsError(generalErrors)

    val result = req
      .inputRequests
      .zipWithIndex
      .toVector
      .map { case (req, i) => (req, res(i)) }
    
    val withAdapterErrors = adaptErrors(result, errorStrategy)
    
    withAdapterErrors.map { case (req, res) => 
      Response(
        req, 
        res.rows.get.filter(data => !(data.lift(0) == "Data Unavailable".some))
      ) 
    }

  private def adaptErrors(result: Vector[(Mnemonic, MnemonicResponse)], errorStrategy: ErrorStrategy) =
     errorStrategy match
      case ErrorStrategy.DiscardAll => 
        val perMnemonicErrors = collectPerMnemonicErrors(result)
        if !perMnemonicErrors.isEmpty then throw MnemonicsError(perMnemonicErrors)

        result

      case ErrorStrategy.DiscardOnlyInvalid => 
        result.map {
          case (mnemonic, res @ MnemonicResponse(errMsg, rows)) if !errMsg.isEmpty =>      
            val errorRow = Vector(s"[Error: $errMsg]")
          
            (
              mnemonic, 
              res.copy(
                rows = rows
                  .map(_.map(_ => errorRow))
                  .orElse(Vector(errorRow).some)
              )
            )

          case other => other
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

  enum ErrorStrategy:
    case DiscardAll, DiscardOnlyInvalid
  