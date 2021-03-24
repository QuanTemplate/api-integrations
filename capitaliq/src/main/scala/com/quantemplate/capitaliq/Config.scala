package com.quantemplate.capitaliq

import com.typesafe.config.ConfigFactory

case class Config(capitaliq: Config.CapitalIQ)
object Config:
  case class CapitalIQ(
    endpoint: String, credentials: CapitalIQ.Credentials
  )
  object CapitalIQ: 
    case class Credentials(username: String, password: String)

  def load() = 
    val conf = ConfigFactory.load

    Config(
      CapitalIQ(
        endpoint = conf.getString("capitaliq.endpoint"),
        credentials = CapitalIQ.Credentials(
          conf.getString("capitaliq.credentials.username"),
          conf.getString("capitaliq.credentials.password")
        )
      )
    )
  