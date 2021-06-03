package com.quantemplate.integrations.commands.addresscleansing

import io.circe.Decoder
import cats.syntax.apply.given
import com.quantemplate.integrations.commands.ConfigDef

case class AddressCleansingConfigDef(
  orgId: String,
  source: Source,
  target: Target
) extends ConfigDef derives Decoder

case class Source(pipeline: Source.PipelineSource) derives Decoder
object Source: 
 

  case class PipelineSource(
    pipelineId: String, 
    outputName: String, 
    dataColumn: Option[String],
    idColumn: Option[String]
  ) derives Decoder

case class Target(
  dataset: String,
  onFinished: Option[Target.Triggers]
) derives Decoder

object Target:
  type Triggers = Vector[Target.Trigger]

  enum Trigger:
    case ExecutePipeline(pipelineId: String)
  
  given Decoder[Trigger] = Decoder { c =>
    c.get[String]("action").flatMap {
      case "ExecutePipeline" => 
        c.get[String]("pipelineId").map(Trigger.ExecutePipeline(_))
    }
  }
  