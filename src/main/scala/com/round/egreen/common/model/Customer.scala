// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.common.model

import java.util.UUID

final case class CustomerUser(user: User, info: CustomerInfo)

final case class CustomerInfo(
    userId: UUID,
    fullName: String,
    phoneNumber: String,
    address: String,
    district: District
)
