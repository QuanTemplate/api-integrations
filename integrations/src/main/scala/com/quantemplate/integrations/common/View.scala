package com.quantemplate.integrations.common

import java.io.ByteArrayOutputStream
import com.norbitltd.spoiwo.model.{Row, Sheet, Workbook}
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions.*

trait View:
  def toBytes: Array[Byte]

object View:
  val blankCell = ""

  type ReportRows = Vector[Vector[Option[String]]]

  case class SheetModel(name: String, rows: ReportRows):
    def prependColumns(cols: Vector[String]*) =
      copy(
        rows = rows.zipWithIndex.map { (row, i) =>
          cols.foldLeft(row)((acc, col) => col.lift(i) +: acc)
        }
      )

class Xlsx(sheets: Vector[View.SheetModel]) extends View:
  import View.*

  lazy val workbook = Workbook(
    sheets.map { m =>
      Sheet(name = m.name)
        .withRows(m.rows.map(row => Row().withCellValues(row.map(_.getOrElse(blankCell)): _*)): _*)
    }: _*
  )

  def toBytes =
    val stream = new ByteArrayOutputStream()
    workbook.writeToOutputStream(stream)

    stream.toByteArray

  def toFile(filePath: String) =
    workbook.saveAsXlsx(filePath)
