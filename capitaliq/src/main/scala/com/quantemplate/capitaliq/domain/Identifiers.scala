package com.quantemplate.capitaliq.domain

import scala.io.Source

import com.quantemplate.capitaliq.Config

object Identifiers:
  val commentToken = "//"

  def load()(using conf: Config) = 
    Source
      .fromResource("identifiers.txt")
      .getLines
      .filter(!_.startsWith(commentToken))
      .map(CapitalIQ.Identifier(_))
      .toVector