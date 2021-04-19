package com.quantemplate.capitaliq.domain

import scala.io.Source

import CapitalIQ.Identifier

object Identifiers:
  def apply(ids: String*) = 
    ids
      .filter(Identifier.isValid)
      .map(Identifier(_))
      .toVector

  // (!) assuming the dataset uses ',' as a separator
  //  and the identifiers are in the first column
  def loadFromCsvString(str: String) =
    Identifiers(
      str
        .split('\n')
        .toVector
        .map(_.split(',').headOption.getOrElse("")): _*
    )
