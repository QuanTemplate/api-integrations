package com.quantemplate.integrations.commands.addresscleansing

import io.circe.Decoder
import cats.syntax.apply.given
import com.quantemplate.integrations.commands.ConfigDef

case class AddressCleansingConfigDef(
  orgId: String,
  source: Source,
  target: Target
) extends ConfigDef

object AddressCleansingConfigDef:
  given Decoder[AddressCleansingConfigDef] = Decoder { c => 
    (
      c.get[String]("orgId"),
      c.get[Source]("source"),
      c.get[Target]("target")
    ).mapN(AddressCleansingConfigDef.apply)
  }

case class Source(
  pipeline: Source.PipelineSource,
  triggeredBy: Option[Vector[Source.Notification]]
)

object Source: 
  enum Notification:
    case DatasetUpdated(datasetId: String)
    case PipelineCompleted(pipelineId: String)

  given Decoder[Notification] = Decoder { c => 
     c.get[String]("action").flatMap {
      case "DatasetUpdated" => 
        c.get[String]("datasetId").map(Notification.DatasetUpdated(_))

      case "PipelineCompleted" => 
        c.get[String]("pipelineId").map(Notification.PipelineCompleted(_))
    }
  }

  case class PipelineSource(
    pipelineId: String, 
    outputName: String, 
    dataColumn: Option[String],
    idColumn: Option[String]
  )
  object PipelineSource:
    given Decoder[PipelineSource] = Decoder { c => 
      (
        c.get[String]("pipelineId"),
        c.get[String]("outputName"),
        c.get[Option[String]]("dataColumn"),
        c.get[Option[String]]("idColumn")
      ).mapN(PipelineSource.apply)
    }

  given Decoder[Source] = Decoder { c => 
    (
      c.get[PipelineSource]("pipeline"),
      c.get[Option[Vector[Notification]]]("triggeredBy")
    ).mapN(Source.apply)
  }


case class Target(
  dataset: String,
  onFinished: Option[Vector[Target.Trigger]]
)

object Target:
  enum Trigger:
    case ExecutePipeline(pipelineId: String)
  
  given Decoder[Trigger] = Decoder { c =>
    c.get[String]("action").flatMap {
      case "ExecutePipeline" => 
        c.get[String]("pipelineId").map(Trigger.ExecutePipeline(_))
    }
  }
  
  given Decoder[Target] = Decoder { c => 
    (
      c.get[String]("dataset"),
      c.get[Option[Vector[Trigger]]]("onFinished")
    ).mapN(Target.apply)
  }

  