package com.quantemplate.capitaliq.commands.mutlidatapointreport

import java.nio.file.Path
import java.time.format.DateTimeFormatter
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.concurrent.Future
import org.slf4j.LoggerFactory
import cats.syntax.traverse.given
import cats.syntax.option.*

import com.quantemplate.capitaliq.common.{Config, HttpService}
import com.quantemplate.capitaliq.domain.CapitalIQService
import com.quantemplate.capitaliq.domain.CapitalIQ.Mnemonic.*
import com.quantemplate.capitaliq.qt.QTService

import com.quantemplate.capitaliq.commands.IdentifierLoader


class MultiDataPointReportCmd:
  private given Config = Config.load()
  private given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  private given ExecutionContext = system.executionContext

  private lazy val logger = LoggerFactory.getLogger(getClass)

  private val httpService = HttpService()
  private val qtService = QTService(httpService)
  private val identifiersLoader = IdentifierLoader(qtService)
  private val capIqService = CapitalIQService(httpService)
  private val multiDataReport = MultiDataPointReport(capIqService, qtService)

  def fromConfigFile(config: MultiPointReportConfigDef, configPath: Path) =
    identifiersLoader
      .loadIdentifiersFromConfig(config.identifiers, configPath, config.orgId)
      .map(_.getOrElse(identifiersLoader.loadIdentifiersFromStdin()))
      .map(config.toCmdConfig(_))
      .map(run)

  private def run(config: CmdConfig) =
    multiDataReport.generateSpreadSheet(config)
