package com.quantemplate.capitaliq.domain

import scala.io.Source

object Identifiers:
  val commentToken = "//"

  def apply(ids: String*) = 
    ids.map(CapitalIQ.Identifier(_)).toVector
    
  def loadFromStdin() = 
    Source.stdin.getLines
      .filter(!_.startsWith(commentToken))
      .map(CapitalIQ.Identifier(_))
      .toVector
