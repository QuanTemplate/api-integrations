package com.quantemplate.capitaliq.domain

import munit.FunSuite
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import cats.syntax.option.*

import com.quantemplate.capitaliq.common.{Config, HttpService}
import com.quantemplate.capitaliq.domain.CapitalIQ.*
import com.quantemplate.capitaliq.domain.CapitalIQService.*

class CapitalIQServiceSpec extends FunSuite:
  test("the response adaptation") {
    new Context():
      run {
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
  }

class Context:
  given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "test-system")
  given ExecutionContext = system.executionContext
  given Config = Config(
    capitaliq = Config.CapitalIQ(
      endpoint = "http://",
      mnemonicsPerRequest = 100,
      credentials = Config.CapitalIQ.Credentials("username", "password"),
    ),
    quantemplate = Config.Quantemplate(
      auth = Config.Quantemplate.Auth("http://", "clientid", "clientSecret"),
      api = Config.Quantemplate.Api("http://")
    )
  )

  def run[T](code: => T) =  
    try
      code
    finally
      system.terminate()

