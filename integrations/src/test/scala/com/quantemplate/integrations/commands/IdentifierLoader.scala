package com.quantemplate.integrations.commands

import munit.FunSuite
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import akka.actor.typed.ActorSystem
import java.nio.file.Paths
import cats.syntax.option.*

import com.quantemplate.integrations.qt.QTService
import com.quantemplate.integrations.capitaliq.CapitalIQ.Identifier
import com.quantemplate.ActorSystemSuite
import IdentifierLoader.*

class IdentifierLoaderSpec extends ActorSystemSuite:
  test("given a column name it should properly load the identifiers from QT dataset") {
    val mockQtService = mock(classOf[QTService])

    val columnName = "Company ID"

    when(mockQtService.downloadDataset(any(), any())) thenReturn {
      Future.successful(
        s"""Input Insured Name,Company Name,$columnName
            |Prestige International Holding,Prestige International Holding,
            |Prestige International Holding,Prestige International Holding,
            |Ministry of Defence of Slovak Republic,Ministry of Defence of Slovak Republic,
            |Ministry of Defence of Slovak Republic,Ministry of Defence of Slovak Republic,
            |CAAC,China Civil Aviation Institute,IQ31234420
            |CAAC,China Civil Aviation Institute1,IQ31234421
            |CAAC,China Civil Aviation Institute2,IQ31234422
            |CAAC,China Civil Aviation Institute3,IQ31234423
          """.stripMargin
      )
    }

    val config = IdentifiersConf(
      dataset = DatasetSource(
        id = "d-abcd", 
        columnName = columnName.some
      ).some 
    )
    
    for
      result <- IdentifierLoader(mockQtService)
        .loadIdentifiersFromConfig(
          config = config.some,
          configPath = Paths.get("/"),
          orgId = "c-qed-insur"
        )

      expected = Vector(
        Identifier("IQ31234420"),
        Identifier("IQ31234421"),
        Identifier("IQ31234422"),
        Identifier("IQ31234423")
      ).some

      _ = assertEquals(result, expected)
    yield ()
  }

  test("given a `limit` config option it should filter out redundant ids") {
    val mockQtService = mock(classOf[QTService])

    val ids = Vector(
      Identifier("IQ31234420"),
      Identifier("IQ31234421"),
      Identifier("IQ31234422"),
      Identifier("IQ31234423")
    )

    val limit = 2

    val config = IdentifiersConf(
      inline = ids.some,
      limit = limit.some
    )

    for
      result <- IdentifierLoader(mockQtService)
        .loadIdentifiersFromConfig(
          config = config.some,
          configPath = Paths.get("/"),
          orgId = "c-qed-insur"
        )

      expected = ids.take(limit).some

      _ = assertEquals(result, expected)
    yield ()
  }

   test("given a `distinct` config option it should filter out duplicated ids") {
    val mockQtService = mock(classOf[QTService])

    val ids = Vector(
      Identifier("IQ31234420"),
      Identifier("IQ31234420"),
      Identifier("IQ31234420"),
      Identifier("IQ31234423")
    )

    val config = IdentifiersConf(
      inline = ids.some,
      distinct = true
    )

    for
      result <- IdentifierLoader(mockQtService)
        .loadIdentifiersFromConfig(
          config = config.some,
          configPath = Paths.get("/"),
          orgId = "c-qed-insur"
        )

      expected = Vector(
        Identifier("IQ31234420"),
        Identifier("IQ31234423")
      ).some

      _ = assertEquals(result, expected)
    yield ()
  }
