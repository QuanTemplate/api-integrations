package com.quantemplate.capitaliq.commands

import java.time.LocalDate
import io.circe.{ Encoder, Decoder, Json, DecodingFailure }
import io.circe.yaml.{parser as ymlParser}
import io.circe.syntax.given
import cats.syntax.apply.given
import cats.instances.either.given
import scopt.OParser

import com.quantemplate.capitaliq.common.*
import com.quantemplate.capitaliq.domain.CapitalIQ.Identifier
import com.quantemplate.capitaliq.domain.Identifiers
import com.quantemplate.capitaliq.commands.revenuereport.*

object ConfigDefInterpreterCmd:
  def fromCli(args: Array[String]) = ConfigArgsParser.parse(args).map { cliConfig =>

    val ymlPath = getPath(cliConfig.path)
    val yml = os.read(ymlPath)

    parseYml(yml).map {
      case config: RevenueReportConfigDef => RevenueReportCmd().fromConfigFile(config, ymlPath)
    }
  }

private def parseYml(yml: String) =
  ymlParser.parse(yml).flatMap(_.as[ConfigDef]).toOption

given Decoder[ConfigDef] = Decoder.instance[ConfigDef] { c => 
  c.get[String]("command").flatMap { 
    case configDefInterpreterCmdName => c.get[RevenueReportConfigDef]("params")
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
      opt[String](configDefInterpreterCmdName)
        .action((path, c) => c.copy(path = path))
        .required
        .text("Runs the command described in a file at a given path")
    )

