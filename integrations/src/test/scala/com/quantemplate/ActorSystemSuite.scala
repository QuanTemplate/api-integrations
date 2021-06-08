package com.quantemplate

import munit.FunSuite
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.ExecutionContext

class ActorSystemSuite extends FunSuite:
  private val systemName = "test-actor-system"
  private val actorSystem = new Fixture[ActorSystem[Nothing]](systemName):
    private var system: ActorSystem[Nothing] = null

    def apply() = system

    override def beforeAll() =
      system = ActorSystem(Behaviors.empty, systemName)

    override def afterAll() =
      system.terminate()

  protected given sys: ActorSystem[Nothing] = actorSystem()
  protected given ExecutionContext = sys.executionContext

  override def munitFixtures = Seq(actorSystem)
