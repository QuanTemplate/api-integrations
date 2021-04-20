package com.quantemplate.capitaliq

import org.slf4j.LoggerFactory

import com.quantemplate.capitaliq.commands.*
import com.quantemplate.capitaliq.commands.revenuereport.*

object Main:
  lazy val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = args match
    case Array(`configDefInterpreterCmdName`, _*) => ConfigDefInterpreterCmd.fromCli(args)
    case Array(`revenueReportCmdName`, _*)        => RevenueReportCmd().fromCli(args)
    case _ => 
      logger.error("Unsupported command")
      System.exit(1)
