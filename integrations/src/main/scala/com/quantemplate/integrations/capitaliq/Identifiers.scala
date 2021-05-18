package com.quantemplate.integrations.capitaliq

import scala.io.Source

import CapitalIQ.Identifier

object Identifiers:
  def apply(ids: String*) =
    ids
      .filter(Identifier.isValid)
      .map(Identifier(_))
      .toVector
