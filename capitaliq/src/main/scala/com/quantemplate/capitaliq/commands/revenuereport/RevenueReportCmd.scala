package com.quantemplate.capitaliq.commands.revenuereport

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import com.quantemplate.capitaliq.common.*
import com.quantemplate.capitaliq.domain.*
import com.quantemplate.capitaliq.qt.*


class RevenueReportCmd:
  given Config = Config.load()
  given sys: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  given ExecutionContext = sys.executionContext

  val logger = sys.log
  val httpService = HttpService()
  val qtService = QTService(httpService)
  val revenueReport = RevenueReport(CapitalIQService(httpService), qtService)

  def fromCli(args: Array[String]) =
    RevenueReportArgsParser.parse(args).map(_.toCmdConfig).map(run)

  def fromConfigFile(config: RevenueReportConfigDef, configPath: os.Path) =
    val inlineIds = config.identifiers.flatMap(_.inline)
    val localIds = config.identifiers.flatMap(_.local).map { rawPath => 
      Identifiers(os.read.lines(getPath(rawPath, configPath)): _*)
    }

    // todo: load the ids from QT dataset
    val remoteIds: Option[Vector[CapitalIQ.Identifier]] = ???

    val identifiers = (inlineIds ++ localIds ++ remoteIds).reduceOption(_ ++ _)

    run(config.toCmdConfig(identifiers))

  private def run(config: CmdConfig) = 
    revenueReport
      .generateSpreadSheet(
        ids = config.identifiers.getOrElse {
          logger.info("Loading the Capital IQ identifiers from the STDIN")

          Identifiers.loadFromStdin()
          },
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

