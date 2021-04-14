package com.quantemplate.capitaliq.common

import com.typesafe.config.ConfigFactory

case class Config(
  capitaliq: Config.CapitalIQ,
  quantemplate: Config.Quantemplate
)
object Config:
  case class CapitalIQ(
    endpoint: String, 
    credentials: CapitalIQ.Credentials,
    mnemonicsPerRequest: Int
  )
  object CapitalIQ: 
    case class Credentials(username: String, password: String)

  case class Quantemplate(
    auth: Quantemplate.Auth,
    api: Quantemplate.Api
  )

  object Quantemplate:
    case class Auth(endpoint: String, clientId: String, clientSecret: String)
    case class Api(baseUrl: String)


  def load() = 
    val conf = ConfigFactory.load

    Config(
      CapitalIQ(
        endpoint = conf.getString("capitaliq.endpoint"),
        credentials = CapitalIQ.Credentials(
          conf.getString("capitaliq.credentials.username"),
          conf.getString("capitaliq.credentials.password"),
        ),
        mnemonicsPerRequest = conf.getInt("capitaliq.mnemonicsPerRequest")
      ),
      Quantemplate(
        auth = Quantemplate.Auth(
          endpoint = conf.getString("quantemplate.auth.endpoint"),
          clientId = conf.getString("quantemplate.auth.credentials.clientId"),
          clientSecret = conf.getString("quantemplate.auth.credentials.clientSecret")
        ),
        api = Quantemplate.Api(
          baseUrl = conf.getString("quantemplate.api.baseUrl")
        )
      )
    )
  