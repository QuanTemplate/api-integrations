package com.quantemplate.capitaliq.commands

import munit.FunSuite
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import akka.actor.typed.ActorSystem
import java.nio.file.Paths
import cats.syntax.option.*

import com.quantemplate.capitaliq.qt.QTService
import com.quantemplate.capitaliq.domain.CapitalIQ.Identifier
import com.quantemplate.ActorSystemSuite
import IdentifierLoader.*

class IdentifierLoaderSpec extends ActorSystemSuite:
  test("should properly load the identifiers from QT dataset") {
    given sys: ActorSystem[Nothing] = actorSystem()
    given ExecutionContext = sys.executionContext

    val mockQtService = mock(classOf[QTService])

    when(mockQtService.downloadDataset(any(), any())) thenReturn {
      Future.successful(
        """Input Insured Name,Company Name,Company ID
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
        columnName = "Company ID".some
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
