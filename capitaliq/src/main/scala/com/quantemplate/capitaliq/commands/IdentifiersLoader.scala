package com.quantemplate.capitaliq.commands

import java.nio.file.Path
import org.slf4j.LoggerFactory
import io.circe.Decoder
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext
import io.circe.syntax.given
import cats.syntax.apply.given
import cats.syntax.traverse.given

import com.quantemplate.capitaliq.common.*
import com.quantemplate.capitaliq.domain.*
import com.quantemplate.capitaliq.qt.*

class IdentifierLoader(qtService: QTService)(using ExecutionContext):
  import IdentifierLoader.*

  private lazy val logger = LoggerFactory.getLogger(getClass)

  def loadIdentifiersFromStdin() =
    logger.info("Loading the Capital IQ identifiers from the STDIN")

    IO.stdin(_.getLines.toVector) match
      case Success(ids) => Identifiers(ids: _*)
      case Failure(err) =>
        logger.error("Could not load the Capital IQ identifiers from the STDIN. Aborting.") 
        throw err
  
  def loadIdentifiersFromConfig(
    config: Option[IdentifiersConf], 
    configPath: Path, 
    orgId: String
  ) =
    config
      .flatMap(_.dataset)
      .map(loadIdentifiersFromDataset(orgId))
      .sequence
      .map { remoteIds => 
        val inlineIds = config.flatMap(_.inline)
        val localIds = config
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

object IdentifierLoader:
  case class IdentifiersConf(
    local: Option[String],
    dataset: Option[String],
    inline: Option[Vector[CapitalIQ.Identifier]]
  )

  object IdentifiersConf:
    given Decoder[IdentifiersConf] = Decoder { c => 
      (
        c.get[Option[String]]("local"),
        c.get[Option[String]]("dataset"),
        c.get[Option[Vector[CapitalIQ.Identifier]]]("inline")
      ).mapN(IdentifiersConf.apply)
    }
  
