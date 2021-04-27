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
      

  private def loadIdentifiersFromDataset(orgId: String)(dataset: DatasetSource) =
    qtService.downloadDataset(orgId, dataset.id)
      .map(loadIdentifiersFromCsvString(dataset.columnName))
      .map { ids => 
        logger.info("Loaded CapitalIQ identifiers from remote dataset")
        ids
      }
      .recover { 
        case e: Throwable => 
          logger.warn(s"Could not load the CapitalIQ identifiers from the remote dataset. Defaulting to local ones.\n$e")
          Vector.empty[CapitalIQ.Identifier]
      }

  private def loadIdentifiersFromCsvString(columnName: Option[String])(str: String) =
    // assuming `,` is the separator
    val table = str.split('\n').toVector.map(_.split(',').toVector)

    println(("str", str))

    val columnIndex = table.lift(0)
      .flatMap(
        _.indexOf(columnName) match  
          case -1 => None 
          case n => Some(n)
      )
      .getOrElse(0)

    val ids = table.map(row => row(columnIndex))

    // println(("ids", ids))

    Identifiers(ids: _*)

object IdentifierLoader:
  case class IdentifiersConf(
    local: Option[LocalSource],
    dataset: Option[DatasetSource],
    inline: Option[InlineSource]
  )
  object IdentifiersConf:
    given Decoder[IdentifiersConf] = Decoder { c => 
      (
        c.get[Option[LocalSource]]("local"),
        c.get[Option[DatasetSource]]("dataset"),
        c.get[Option[InlineSource]]("inline")
      ).mapN(IdentifiersConf.apply)
    }

  type LocalSource = String
  type InlineSource = Vector[CapitalIQ.Identifier]

  case class DatasetSource(id: String, columnName: Option[String] = None)
  object DatasetSource:
    given Decoder[DatasetSource] = 
      Decoder[String].map(DatasetSource(_)) or 
      Decoder { c => 
        (
          c.get[String]("datasetId"), 
          c.get[Option[String]]("columnName")
        ).mapN(DatasetSource.apply)
      }
    
  