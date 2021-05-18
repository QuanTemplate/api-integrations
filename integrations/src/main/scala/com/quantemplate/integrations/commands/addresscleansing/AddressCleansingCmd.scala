package com.quantemplate.integrations.commands.addresscleansing


import java.nio.file.Path
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import org.slf4j.LoggerFactory

import com.quantemplate.integrations.common.{Config, HttpService}
import com.quantemplate.integrations.capitaliq.CapitalIQService
import com.quantemplate.integrations.qt.QTService

import com.quantemplate.integrations.commands.IdentifierLoader


class AddressCleansingCmd:
  // private given Config.CapitalIQ = Config.CapitalIQ.load()
  // private given Config.Quantemplate = Config.Quantemplate.load()
  private given Config.GoogleMaps = Config.GoogleMaps.load()
  private given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "api-integrations")
  private given ExecutionContext = system.executionContext

  private lazy val logger = LoggerFactory.getLogger(getClass)

  // private val httpService = HttpService()
  // private val qtService = QTService(httpService)
  // private val identifiersLoader = IdentifierLoader(qtService)
  // private val capIqService = CapitalIQService(httpService)
  // private val multiDataReport = MultiDataPointReport(capIqService, qtService)

  def fromConfigFile(config: AddressCleansingConfigDef) = 
    println("hello world")
    // identifiersLoader
    //   .loadIdentifiersFromConfig(config.identifiers, configPath, config.orgId)
    //   .map(_.getOrElse(identifiersLoader.loadIdentifiersFromStdin()))
    //   .map(config.toCmdConfig(_))
    //   .map(run)

  // private def run(config: CmdConfig) =
    // if config.identifiers.isEmpty then 
    //   logger.error("No valid CapitalIQ identifiers were provided. Aborting")
    //   Runtime.getRuntime.halt(1)

    // multiDataReport
    //   .generateSpreadSheet(config)
    //   .onComplete { 
    //     case Failure(e) => 
    //       logger.error("Failed to generate a multi data point report", e)
    //       Runtime.getRuntime.halt(1)

    //     case Success(_) =>
    //       Runtime.getRuntime.halt(0)
    //   }
