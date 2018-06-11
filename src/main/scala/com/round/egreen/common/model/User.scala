// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.common.model

final case class User(
    username: String,
    encryptedPassword: String,
    roles: Set[Role]
)
