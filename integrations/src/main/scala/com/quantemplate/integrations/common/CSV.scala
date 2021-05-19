package com.quantemplate.integrations.common

import cats.syntax.functorFilter.given

object CSV:
  def dataFromColumn(str: String, columnName: Option[String]): Vector[String] =
    val separator = ','
    val table = str.split('\n').toVector.map(_.split(separator).toVector)

    val namedColumnIndex = 
      for 
        firstRow <- table.lift(0)
        name <- columnName
        index <- firstRow.indexOf(name) match 
          case -1 => None
          case n => Some(n) 
      yield index

    val columnIndex = namedColumnIndex getOrElse 0

    table.mapFilter(_.lift(columnIndex))
