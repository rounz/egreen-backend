// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.common.model

import java.util.UUID

final case class User(
    id: UUID,
    username: String,
    encryptedPassword: String,
    roles: Set[Role]
)
