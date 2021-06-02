package com.quantemplate.integrations.qt

import io.circe.{Encoder, Decoder}
import cats.syntax.apply.given

object QTModels:
  case class TokenResponse(accessToken: String)
  object TokenResponse:
    given Decoder[TokenResponse] = Decoder.forProduct1("access_token")(TokenResponse(_))

  case class PipelineExecutionResponse(
    id: String,
    runNumber: Int,
    version: Int,
  )
  object PipelineExecutionResponse:
    given Decoder[PipelineExecutionResponse] = Decoder { c => 
      (
        c.get[String]("id"),
        c.get[Int]("runNumber"),
        c.get[Int]("version")
      ).mapN(PipelineExecutionResponse.apply)
    }

  type ExecutionStatus =  "Started" | "Succeeded" | "Failed" | "Canceled"
  given Decoder[ExecutionStatus] = Decoder.decodeString
    .ensure(
      a => Seq("Started", "Succeeded", "Failed", "Canceled").exists(_ == a),
      "Not a valid status"
      )
    .map(_.asInstanceOf[ExecutionStatus]) 

  case class ExecutionOutput(
    id: String,
    name: String
  )
  object ExecutionOutput:
    given Decoder[ExecutionOutput] = Decoder { c => 
      (
        c.get[String]("id"),
        c.get[String]("name")
      ).mapN(ExecutionOutput.apply)
    }
  
  case class Execution(
     id: String,
     status: ExecutionStatus,
     runNumber: Int,
     version: Int,
     outputs: Vector[ExecutionOutput] // empty if not finished
  )
  object Execution:
    given Decoder[Execution] = Decoder { c => 
      (
        c.get[String]("id"),
        c.get[ExecutionStatus]("status"),
        c.get[Int]("runNumber"),
        c.get[Int]("version"),
        c.get[Vector[ExecutionOutput]]("outputs")
      ).mapN(Execution.apply)
    }

