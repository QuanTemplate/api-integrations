package com.quantemplate.capitaliq.domain

import scala.io.Source

import com.quantemplate.capitaliq.Config

object Identifiers:
  val commentToken = "//"

  def apply(ids: String*) = 
    ids.map(CapitalIQ.Identifier(_)).toVector
    
  def loadFromStdin() = 
    Source.stdin.getLines
      .filter(!_.startsWith(commentToken))
      .map(CapitalIQ.Identifier(_))
      .toVector
