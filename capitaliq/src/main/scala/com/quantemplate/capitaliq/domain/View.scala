package com.quantemplate.capitaliq.domain

import scala.util.Try

import com.norbitltd.spoiwo.model.{Row, Sheet, Workbook}
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions.*

object View:
  lazy val blankCell = "-"

  def xlsx(fileName: String, sheets: Vector[SheetModel]) =
     Workbook(
        sheets.map { m => 
          Sheet(name = m.name)
            .withRows(m.rows.map(row => Row().withCellValues(row.map(_.getOrElse(blankCell)): _*)): _*)
        }: _*
      ).saveAsXlsx(s"/Users/gbielski/Projects/qt/data-ingress/$fileName.xlsx") 
  
  case class SheetModel(name: String, rows: Vector[Vector[Option[String]]])
