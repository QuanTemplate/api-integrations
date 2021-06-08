package com.quantemplate.integrations.common

import com.typesafe.config.ConfigFactory

object Config:
  case class CapitalIQ(
      endpoint: String,
      credentials: CapitalIQ.Credentials,
      mnemonicsPerRequest: Int
  )
  object CapitalIQ:
    case class Credentials(username: String, password: String)

    def load(): CapitalIQ =
      val conf = ConfigFactory.load("capitaliq")

      CapitalIQ(
        endpoint = conf.getString("capitaliq.endpoint"),
        credentials = CapitalIQ.Credentials(
          conf.getString("capitaliq.credentials.username"),
          conf.getString("capitaliq.credentials.password")
        ),
        mnemonicsPerRequest =
          if conf.getBoolean("capitaliq.demoAccount") then conf.getInt("capitaliq.mnemonicsPerRequestInDemoAccount")
          else conf.getInt("capitaliq.mnemonicsPerRequestInProdAccount")
      )

  case class Quantemplate(
      auth: Quantemplate.Auth,
      api: Quantemplate.Api
  )

  object Quantemplate:
    case class Auth(endpoint: String, clientId: String, clientSecret: String)
    case class Api(baseUrl: String)

    def load(): Quantemplate =
      val conf = ConfigFactory.load("quantemplate")

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

  case class GoogleMaps(apiKey: String)
  object GoogleMaps:
    def load(): GoogleMaps =
      val conf = ConfigFactory.load("google-maps")

      GoogleMaps(
        apiKey = conf.getString("googlemaps.apiKey")
      )
