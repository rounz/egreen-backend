// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.cqrs.event

import com.round.egreen.common.model.Role
import io.circe.generic.auto._
import io.circe.syntax._

final case class EventEnvelope(eventName: String, json: String)

object EventEnvelope {
  val LAST_EVENT_NAME: String = "LAST_EVENT"
  val LAST_EVENT: Throwable   = new Throwable(LAST_EVENT_NAME)
}

sealed trait Event {
  def envelope: EventEnvelope
}

final case class CreateUser(
    username: String,
    encryptedPassword: String,
    roles: Set[Role]
) extends Event {

  val envelope: EventEnvelope =
    EventEnvelope(getClass.getName, this.asJson.toString)
}
