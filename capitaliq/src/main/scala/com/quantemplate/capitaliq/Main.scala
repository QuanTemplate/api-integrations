package com.quantemplate.capitaliq

import org.slf4j.LoggerFactory

import com.quantemplate.capitaliq.commands.*

object Main:
  lazy val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = args match
    case Array("generateRevenueReport", _*) => RevenueReportCmd.run(args)
    case _ => 
      logger.error("Unsupported command")
      System.exit(1)
