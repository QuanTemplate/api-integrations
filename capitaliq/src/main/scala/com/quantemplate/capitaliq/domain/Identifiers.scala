package com.quantemplate.capitaliq.domain

import scala.io.Source

import com.quantemplate.capitaliq.Config

object Identifiers:
  def load()(using conf: Config) = 
    Source
      .fromResource("identifiers.txt")
      .getLines
      .map(CapitalIQ.Identifier(_))
      .toVector