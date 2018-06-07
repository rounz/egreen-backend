// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.common.model

sealed trait Role {
  def name: String = getClass.getSimpleName
}

object Developer extends Role
object Admin extends Role
object AdminAssistant extends Role
object Customer extends Role
