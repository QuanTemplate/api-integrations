package com.quantemplate.capitaliq.commands.revenuereport

import java.nio.file.Path
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
  given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  given ExecutionContext = system.executionContext

  lazy val logger = LoggerFactory.getLogger(getClass)

  val httpService = HttpService()
  val qtService = QTService(httpService)
  val revenueReport = RevenueReport(CapitalIQService(httpService), qtService)

  def fromCli(args: Array[String]) =
    RevenueReportArgsParser.parse(args)
      .map(_.toCmdConfig(loadIdentifiersFromStdin()))
      .map(run)

  def fromConfigFile(config: RevenueReportConfigDef, configPath: Path) = 
    loadIdentifiersFromConfig(config, configPath)
      .map(_.getOrElse(loadIdentifiersFromStdin()))
      .map(config.toCmdConfig(_))
      .map(run)

  private def loadIdentifiersFromStdin() = {
    logger.info("Loading the Capital IQ identifiers from the STDIN")

    IO.stdin(_.getLines.toVector) match
      case Success(ids) => Identifiers(ids: _*)
      case Failure(err) =>
        logger.error("Could not load the Capital IQ identifiers from the STDIN. Aborting.") 
        throw err
  }

  private def loadIdentifiersFromConfig(config: RevenueReportConfigDef, configPath: Path) =
    config.identifiers
      .flatMap(_.dataset)
      .map(loadIdentifiersFromDataset(config.orgId))
      .sequence
      .map { remoteIds => 
        val inlineIds = config.identifiers.flatMap(_.inline)
        val localIds = config.identifiers
          .flatMap(_.local)
          .flatMap(loadIdentifiersFromLocalFile(configPath))

        (inlineIds ++ localIds ++ remoteIds).reduceOption(_ ++ _)
      }

  private def loadIdentifiersFromLocalFile(configPath: Path)(rawPath: String) =
    // assuming the config path is a base for resolving the file path of identifiers
    val idsPath = configPath.getParent.resolve(IO.toPath(rawPath))

    IO.readLines(idsPath)
      .recover {
        case err: Throwable =>
          logger.warn("Could not load the Capital IQ identifiers from the local file {}", err)
          Vector.empty[String]
      }
      .toOption
      .map(Identifiers(_: _*))
      

  private def loadIdentifiersFromDataset(datasetId: String)(orgId: String) =
    qtService.downloadDataset(orgId, datasetId)
      .map(Identifiers.loadFromCsvString)
      .map { ids => 
        logger.info("Loaded CapitalIQ identifiers from remote dataset")
        ids
      }
      .recover { 
        case e: Throwable => 
          logger.warn("Could not load the CapitalIQ identifiers from the remote dataset. Defaulting to local ones.", e)
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

