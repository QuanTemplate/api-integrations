package com.quantemplate.integrations.common

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.Logger

import FutureOps.*

def measure[T](codeBlock: => Future[T])
  (using logger: Logger, ec: ExecutionContext): Future[T] =
    val t0 = System.nanoTime()

    codeBlock.tap { result => 
      val t1 = System.nanoTime()

      val elapsed = TimeUnit.SECONDS.convert(t1 - t0, TimeUnit.NANOSECONDS)
      logger.info("Finished in: " + elapsed + "s")
    }
