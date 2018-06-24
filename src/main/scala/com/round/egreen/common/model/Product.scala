// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.common.model

import java.util.UUID

import cats.Order

final case class ProductPackage(
    id: UUID,
    active: Boolean,
    amount: Float,
    frequency: Int, // deliver per week
    price: Int
)

final case class ProductSubscription(
    id: UUID,
    customerId: UUID,
    packageId: UUID,
    startWeek: EgreenWeek,
    endWeek: EgreenWeek,
    totalAmount: Float,
    remainingAmount: Float
)

trait EgreenWeekCompanion { _: EgreenWeek.type =>
  implicit val egreenWeekOrder: Order[EgreenWeek] = Order.from { (x, y) =>
    if (x.year < y.year) -1
    else if (x.year > y.year) 1
    else if (x.week < y.week) -1
    else if (x.week > y.week) 1
    else 0
  }
}
