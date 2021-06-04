package com.quantemplate.integrations.common

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object FutureOps:
  extension [T](f: Future[T])(using ExecutionContext)
    def tap(fn: T => Any): Future[T] =
      f.map { a =>
        fn(a)
        a
      }
