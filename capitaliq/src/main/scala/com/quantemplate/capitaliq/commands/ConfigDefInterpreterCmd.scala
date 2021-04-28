package com.quantemplate.capitaliq.commands

import java.time.LocalDate
import java.nio.file.Path
import io.circe.{ Encoder, Decoder, Json, DecodingFailure }
import io.circe.yaml.{parser as ymlParser}
import org.slf4j.LoggerFactory
import scopt.OParser
import cats.syntax.bifunctor.given

import com.quantemplate.capitaliq.common.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Identifier
import com.quantemplate.capitaliq.domain.Identifiers
import com.quantemplate.capitaliq.commands.revenuereport.*
import com.quantemplate.capitaliq.commands.mutlidatapointreport.*

object ConfigDefInterpreterCmd:
  lazy val logger = LoggerFactory.getLogger(getClass)

  def fromCli(args: Array[String]) = ConfigArgsParser.parse(args).map { cliConfig =>
    val configPath = IO.absolutePath(cliConfig.path)

    loadConfig(configPath).bimap(
      err => logger.error("Could not parse the config file", err),
      {
        case config: RevenueReportConfigDef => 
          RevenueReportCmd().fromConfigFile(config, configPath)
        case config: MultiPointReportConfigDef => 
          MultiDataPointReportCmd().fromConfigFile(config, configPath)
      }
    )
  }

private def loadConfig(path: Path) =
  IO.readAll(path)
    .toEither
    .flatMap(ymlParser.parse(_))
    .flatMap(_.as[ConfigDef])

given Decoder[ConfigDef] = Decoder.instance[ConfigDef] { c => 
  c.get[String]("command").flatMap { 
    case `revenueReportCmdName`    => c.get[RevenueReportConfigDef]("params")
    case `multiPointReportCmdName` => c.get[MultiPointReportConfigDef]("params")
  }
}

val configDefInterpreterCmdName = "apply"

object ConfigArgsParser:
  case class CliConfig(path: String = "")

  def parse(args: Array[String]) = OParser.parse(parser, args, CliConfig())

  private lazy val builder = OParser.builder[CliConfig]
  private lazy val parser =
    import builder.*

    OParser.sequence(
      programName("capitaliq-qt integration"),
      head("capitaliq-qt", "0.0.1"),
      cmd(configDefInterpreterCmdName)
        .text("Runs the command described in a file at a given path")
        .children(
          arg[String]("<config file>...")
            .action((path, c) => c.copy(path = path))
            .unbounded
            .required
        )
    )

