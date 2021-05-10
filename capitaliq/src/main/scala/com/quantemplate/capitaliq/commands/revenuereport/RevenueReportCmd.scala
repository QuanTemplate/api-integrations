package com.quantemplate.capitaliq.commands.revenuereport

import java.nio.file.Path
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.concurrent.Future
import org.slf4j.LoggerFactory
import cats.syntax.traverse.given

import com.quantemplate.capitaliq.common.{Config, HttpService}
import com.quantemplate.capitaliq.domain.CapitalIQService
import com.quantemplate.capitaliq.qt.QTService

import com.quantemplate.capitaliq.commands.IdentifierLoader

class RevenueReportCmd:
  private given Config = Config.load()
  private given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  private given ExecutionContext = system.executionContext

  private lazy val logger = LoggerFactory.getLogger(getClass)

  private val httpService = HttpService()
  private val qtService = QTService(httpService)
  private val identifiersLoader = IdentifierLoader(qtService)
  private val revenueReport = RevenueReport(CapitalIQService(httpService), qtService)

  def fromCli(args: Array[String]) =
    RevenueReportArgsParser.parse(args)
      .map(_.toCmdConfig(identifiersLoader.loadIdentifiersFromStdin()))
      .map(run)

  def fromConfigFile(config: RevenueReportConfigDef, configPath: Path) = 
    identifiersLoader
      .loadIdentifiersFromConfig(config.identifiers, configPath, config.orgId)
      .map(_.getOrElse(identifiersLoader.loadIdentifiersFromStdin()))
      .map(config.toCmdConfig(_))
      .map(run)

  private def run(config: CmdConfig) =
    val ids = config.identifiers

    if ids.isEmpty then 
      logger.error("No valid CapitalIQ identifiers were provided. Aborting")
      Runtime.getRuntime.halt(1)

    revenueReport
      .generateSpreadSheet(
        ids = ids,
        range = (
          config.from,
          config.to
        ),
        currency = config.currency,
        orgId = config.orgId,
        datasetId = config.datasetId
      ).onComplete { 
        case Failure(e) => 
          logger.error("Failed to generate the revenue report", e)
          Runtime.getRuntime.halt(1)

        case Success(_) =>
          Runtime.getRuntime.halt(0)
      }
