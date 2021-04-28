package com.quantemplate

import munit.FunSuite
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

class ActorSystemSuite extends FunSuite:
  val systemName = "test-actor-system"
  val actorSystem = new Fixture[ActorSystem[Nothing]](systemName): 
    private var system: ActorSystem[Nothing] = null

    def apply() = system
    
    override def beforeEach(context: BeforeEach) =
      system = ActorSystem(Behaviors.empty, systemName)

    override def afterEach(context: AfterEach) =
      system.terminate()

  override def munitFixtures = Seq(actorSystem)
