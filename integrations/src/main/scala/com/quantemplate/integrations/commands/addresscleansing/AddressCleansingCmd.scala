package com.quantemplate.integrations.commands.addresscleansing


import java.nio.file.Path
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.given
import akka.actor.Scheduler
import akka.pattern.retry
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.given
import scala.util.{Failure, Success, Using}
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import com.google.maps.model.AddressComponentType.*
import cats.syntax.option.given
import cats.syntax.traverse.given

import com.quantemplate.integrations.common.*
import com.quantemplate.integrations.capitaliq.CapitalIQService
import com.quantemplate.integrations.qt.QTService
import com.quantemplate.integrations.qt.QTModels.*
import com.quantemplate.integrations.commands.IdentifierLoader
import com.quantemplate.integrations.gmaps.GeocodingService

class AddressCleansingCmd:
  import AddressCleansingCmd.*
  import AddressCleanseError.*

  private given Config.GoogleMaps = Config.GoogleMaps.load()
  private given Config.Quantemplate = Config.Quantemplate.load()

  private given logger: Logger = LoggerFactory.getLogger(getClass)
  private given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "api-integrations")
  private given Scheduler = system.scheduler.toClassic
  private given ExecutionContext = system.executionContext

  private val geocodingService = GeocodingService()
  private val qtService = QTService(HttpService())

  def fromConfigFile(config: AddressCleansingConfigDef) =
    import config.{ orgId } 
    import config.source.pipeline.{ pipelineId, outputName, dataColumn, idColumn }
    import config.target.{ dataset as targetDatasetId, onFinished }

    measure {
      for 
        finishedExec <- executePipeline(orgId, pipelineId)
        outputId <- finishedExec.outputs.find(_.name == outputName).map(_.id) toFutureWith OutputIdNotFound

        csvStr <- qtService.downloadPipelineOutput(orgId, pipelineId, finishedExec.id, outputId)
        _ = logger.info("Retrieved the pipeline output")

        sourceAddresses = CSV.dataFromColumn(csvStr, dataColumn)
        sourceAddressesBody = sourceAddresses.drop(1) // dropping column name
        sourceIds = CSV.dataFromColumn(csvStr, idColumn)

        rows <- geocodingService.getGeocodedRows(sourceAddressesBody)
        _ = logger.info("Retrieved the Geocoded result")

        sheet = constructSpreadsheet(rows, sourceAddresses, sourceIds)
        _ <- qtService.uploadDataset(sheet, orgId, targetDatasetId)
        _ = logger.info("Uploaded the spreadsheet")

        _ <- executeTargetTriggers(orgId, onFinished)
      yield ()
    } onComplete { 
      case Failure(e) => 
        logger.error("Failed to cleanse addresses", e)
        Runtime.getRuntime.halt(1)

      case Success(_) =>
        Runtime.getRuntime.halt(0)
    }

  private def constructSpreadsheet(
    rows: Vector[Vector[Option[String]]], 
    sourceAddresses: Vector[String], 
    sourceIds: Vector[String]
  ) =
    val sheetModel = View.SheetModel("Geocoded result", rows)
      .prependColumns(sourceAddresses, sourceIds)

    Xlsx(Vector(sheetModel))

  private def executePipeline(orgId: String, pipelineId: String) = 
    for 
      initExec <- qtService.executePipeline(orgId, pipelineId)
      executionId = initExec.id
      _ = logger.info("Execution for the pipeline {} has started", pipelineId)

      _ = logger.info("Waiting for the {} pipeline to finish execution...", pipelineId)
      finishedExec <- waitForExecutionToFinish(orgId, pipelineId, executionId)
    yield finishedExec
  
  private def waitForExecutionToFinish(orgId: String, pipelineId: String, execId: String) =
    measure {
      retry(
        () => 
          qtService.listExecutions(orgId, pipelineId)
            .map(_.find(_.id == execId).filter(_.status == "Succeeded"))
            .flatMap(_ toFutureWith ExecutionNotFinished)
        ,
        300,
        2.seconds
      )
    }

  private def executeTargetTriggers(
    orgId: String, 
    maybeTriggers: Option[Target.Triggers]
  ): Future[Unit] =
    maybeTriggers
      .map { 
        _
          .traverse {
            case trigger: Target.Trigger.ExecutePipeline => 
              executePipeline(orgId, trigger.pipelineId)
          }
          .map(_ => ())
      }
      .getOrElse(Future.successful(()))


object AddressCleansingCmd:
  extension [T](op: Option[T])
    def toFutureWith(e: Throwable): Future[T] = 
      op.fold(Future.failed(e))(Future.successful)

  enum AddressCleanseError extends Throwable:
    case ExecutionNotFinished
    case OutputIdNotFound
