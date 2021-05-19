package com.quantemplate.integrations.commands.addresscleansing


import java.nio.file.Path
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Using}
import org.slf4j.LoggerFactory
import org.slf4j.Logger

import com.quantemplate.integrations.common.*
import com.quantemplate.integrations.capitaliq.CapitalIQService
import com.quantemplate.integrations.qt.QTService
import com.quantemplate.integrations.qt.QTService.PipelineExecutionResponse

import com.quantemplate.integrations.commands.IdentifierLoader
import com.quantemplate.integrations.gmaps.GeocodingService


class AddressCleansingCmd:
  private given Config.GoogleMaps = Config.GoogleMaps.load()
  private given Config.Quantemplate = Config.Quantemplate.load()
  private given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "api-integrations")
  private given ExecutionContext = system.executionContext

  private given logger: Logger = LoggerFactory.getLogger(getClass)
  private val geocodingService = GeocodingService()
  private val qtService = QTService(HttpService())

  def fromConfigFile(config: AddressCleansingConfigDef) = 
    measure {
      for 
        execRes <- qtService.executePipeline(
          config.orgId, 
          config.source.pipeline.pipelineId
        )

        // wait for execution to finish

        csvStr <- qtService.downloadPipelineOutput(
          config.orgId,
          config.source.pipeline.pipelineId,
          execRes.executionId,
          config.source.pipeline.outputId
        )

        addressColumns = CSV.dataFromColumn(csvStr, config.source.pipeline.column)

        geocodingRes <- geocodingService.geocode(addressColumns)
        _ = println(geocodingRes)

      yield ()
    }.onComplete { 
      case Failure(e) => 
        logger.error("Failed to generate a multi data point report", e)
        Runtime.getRuntime.halt(1)

      case Success(_) =>
        Runtime.getRuntime.halt(0)
    }
  