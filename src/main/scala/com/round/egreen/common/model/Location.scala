// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.common.model

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._

sealed trait DeliveryBatch
object MonThu extends DeliveryBatch
object TueFri extends DeliveryBatch

sealed trait DeliveryZone
object KV1 extends DeliveryZone
object KV2 extends DeliveryZone

sealed abstract class District(
    val id: String,
    val name: String,
    val batch: DeliveryBatch,
    val zone: DeliveryZone
) {

  /** Workaround for `circe`'s empty object marshalling */
  final private case class Repr(name: String, batch: DeliveryBatch, zone: DeliveryZone)

  final val asJson: Json = Json.obj(
    id -> Repr(name, batch, zone).asJson
  )
}

object Quan1 extends District("Quan1", "Quận 1", TueFri, KV1)
object Quan2 extends District("Quan2", "Quận 2", MonThu, KV1)
object Quan3 extends District("Quan3", "Quận 3", TueFri, KV1)
object Quan4 extends District("Quan4", "Quận 4", TueFri, KV1)
object Quan5 extends District("Quan5", "Quận 5", TueFri, KV1)
object Quan6 extends District("Quan6", "Quận 6", TueFri, KV1)
object Quan7 extends District("Quan7", "Quận 7", TueFri, KV1)
object Quan8 extends District("Quan8", "Quận 8", TueFri, KV1)
object Quan9 extends District("Quan9", "Quận 9", MonThu, KV1)
object Quan10 extends District("Quan10", "Quận 10", TueFri, KV1)
object Quan11 extends District("Quan11", "Quận 11", TueFri, KV1)
object Quan12 extends District("Quan12", "Quận 12", MonThu, KV1)
object TanBinh extends District("TanBinh", "Tân Bình", TueFri, KV1)
object TanPhu extends District("TanPhu", "Tân Phú", TueFri, KV1)
object NhaBe extends District("NhaBe", "Nhà Bè", TueFri, KV1)
object BinhChanh extends District("BinhChanh", "Bình Chánh", TueFri, KV1)
object PhuNhuan extends District("PhuNhuan", "Phú Nhuận", TueFri, KV1)
object BinhThanh extends District("BinhThanh", "Bình Thạnh", MonThu, KV1)
object BinhTan extends District("BinhTan", "Bình Tân", MonThu, KV1)
object GoVap extends District("GoVap", "Gò Vấp", MonThu, KV1)
object ThuDuc extends District("ThuDuc", "Thủ Đức", MonThu, KV1)
