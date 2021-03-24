package com.quantemplate.capitaliq.domain

import scala.io.Source

import com.quantemplate.capitaliq.Config

object Identifiers:
  def load()(using conf: Config) = 
    Source
      .fromResource("identifiers.txt")
      .getLines
      .take(
        // TODO: do not skip ids
        // split it per `conf.capitaliq.mnemonicsPerRequest` 
        // and make separate requests
        1
      )
      .map(CapitalIQ.Identifier(_))
      .toSeq