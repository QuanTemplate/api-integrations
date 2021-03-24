package com.quantemplate.capitaliq

import com.typesafe.config.ConfigFactory

case class Config(endpoint: String, credentials: Config.Credentials)
object Config:
  case class Credentials(username: String, password: String)

  def load() = 
    val conf = ConfigFactory.load

    Config(
      endpoint = conf.getString("capitaliq.endpoint"),
      credentials = Credentials(
        conf.getString("capitaliq.credentials.username"),
        conf.getString("capitaliq.credentials.password")
      )
    )
  