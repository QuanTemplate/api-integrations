package com.quantemplate.capitaliq.commands.revenuereport

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.concurrent.Future
import org.slf4j.LoggerFactory
import cats.syntax.traverse.given

import com.quantemplate.capitaliq.common.*
import com.quantemplate.capitaliq.domain.*
import com.quantemplate.capitaliq.qt.*


class RevenueReportCmd:
  given Config = Config.load()
  given sys: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  given ExecutionContext = sys.executionContext

  lazy val logger = LoggerFactory.getLogger(getClass)

  val httpService = HttpService()
  val qtService = QTService(httpService)
  val revenueReport = RevenueReport(CapitalIQService(httpService), qtService)

  def fromCli(args: Array[String]) =
    RevenueReportArgsParser.parse(args)
      .map(_.toCmdConfig(loadIdentifiersFromStdin()))
      .map(run)

  def fromConfigFile(config: RevenueReportConfigDef, configPath: os.Path) = 
    loadIdentifiersFromConfig(config, configPath)
      .map(_.getOrElse(loadIdentifiersFromStdin()))
      .map(config.toCmdConfig(_))
      .map(run)

  private def loadIdentifiersFromStdin() = {
    logger.info("Loading the Capital IQ identifiers from the STDIN")

    Identifiers.loadFromStdin()
  }

  private def loadIdentifiersFromConfig(config: RevenueReportConfigDef, configPath: os.Path) =
    config.identifiers
      .flatMap(_.dataset)
      .map(loadIdentifiersFromDataset(config.orgId, _))
      .sequence
      .map { remoteIds => 
        val inlineIds = config.identifiers.flatMap(_.inline)
        val localIds = config.identifiers.flatMap(_.local).map { rawPath =>
          Identifiers(
            os.read.lines(getPath(rawPath, configPath / os.up)): _*
          )
        }

        (inlineIds ++ localIds ++ remoteIds).reduceOption(_ ++ _)
      }

  private def loadIdentifiersFromDataset(orgId: String, datasetId: String) =
    qtService.downloadDataset(orgId, datasetId)
      .map(Identifiers.loadFromCsvString)
      .recover { 
        case e: Throwable => 
          logger.warn("Could not load the identifiers from the remote dataset")
          println(("err", e))
          e.printStackTrace()

          Vector.empty[CapitalIQ.Identifier]
      }

  private def run(config: CmdConfig) =
    revenueReport
      .generateSpreadSheet(
        ids = config.identifiers.distinct,
        range = (
          config.from,
          config.to
        ),
        currency = config.currency,
        orgId = config.orgId,
        datasetId = config.datasetId
      ).onComplete { 
        case Failure(e) => 
          logger.error("Failed to generate the revenue report: {}", e.toString)
          Runtime.getRuntime.halt(1)

        case Success(_) =>
          Runtime.getRuntime.halt(0)
      }

