package com.quantemplate.capitaliq

import java.time.LocalDate
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import org.slf4j.LoggerFactory

import com.quantemplate.capitaliq.domain.*
import com.quantemplate.capitaliq.qt.*

def withRevenueReport(fn: RevenueReport => Config ?=> Future[Unit]) =
  given Config = Config.load()
  given sys: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "capitaliq")
  given ExecutionContext = sys.executionContext

  lazy val logger = LoggerFactory.getLogger(getClass)

  val httpService = HttpService()
  val revenueReport = RevenueReport(
    CapitalIQService(httpService), 
    QTService(httpService)
  )

  fn(revenueReport) onComplete { 
     case Failure(e) => 
      logger.error("Failed to generate the revenue report: {}", e.toString)
      Runtime.getRuntime.halt(1)

     case Success(_) =>
      Runtime.getRuntime.halt(0)
  }
  
