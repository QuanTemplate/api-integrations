package com.quantemplate.integrations.capitaliq

import munit.FunSuite
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import akka.actor.typed.ActorSystem
import cats.syntax.option.*

import com.quantemplate.integrations.common.{Config, HttpService}
import com.quantemplate.integrations.capitaliq.CapitalIQ.*
import com.quantemplate.integrations.capitaliq.CapitalIQService.*
import com.quantemplate.ActorSystemSuite

class CapitalIQServiceSpec extends ActorSystemSuite:
  test("it should properly adapt a valid response") {
    given Config = Config.load()

    val mockHttpService = mock(classOf[HttpService])

    val mnemonic = Mnemonic.IQ_COMPANY_NAME_LONG(Identifier("IQ12334"))
    val rows = Vector(Vector("PZU"))

    val rawResponse = RawResponse(
      Vector(RawResponse.MnemonicResponse("", rows.some))
    )

    when(
      mockHttpService.post[Request, RawResponse](any(), any(), any())(any(), any())
    ).thenReturn(Future.successful(rawResponse))
    
    val service = CapitalIQService(mockHttpService)

    for 
      result <- service.sendRequest(Request(Vector(mnemonic)))
      _ = assertEquals(result, Vector(Response(mnemonic, rows)))
    yield ()
  }
