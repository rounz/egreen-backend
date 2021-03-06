// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.cqrs.event

import cats.implicits._
import scalapb.GeneratedMessage

import scala.reflect.ClassTag

trait EventEnvelopeCompanion { _: EventEnvelope.type =>
  val LAST_EVENT_NAME: String = "LAST_EVENT"
  val LAST_EVENT: Throwable   = new Throwable(LAST_EVENT_NAME)
}

trait Event { _: GeneratedMessage =>

  def envelope: EventEnvelope =
    EventEnvelope(System.currentTimeMillis.some, getClass.getName.some, toByteString.some)
}

trait EventCompanion[E <: Event] {
  def eventName(implicit E: ClassTag[E]): String = E.runtimeClass.getName
}
