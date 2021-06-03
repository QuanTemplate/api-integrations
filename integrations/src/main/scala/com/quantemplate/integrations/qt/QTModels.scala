package com.quantemplate.integrations.qt

import io.circe.Decoder
import cats.syntax.apply.given

object QTModels:
  case class TokenResponse(accessToken: String) derives Decoder

  case class PipelineExecutionResponse(
    id: String,
    runNumber: Int,
    version: Int,
  ) derives Decoder

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
  ) derives Decoder

  case class Execution(
     id: String,
     status: ExecutionStatus,
     runNumber: Int,
     version: Int,
     outputs: Vector[ExecutionOutput] // empty if not finished
  ) derives Decoder
