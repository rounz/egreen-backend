// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen

import java.util.UUID

import com.round.egreen.common.model.Role
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import scalapb.TypeMapper

package object common {
  implicit val uuidTMap: TypeMapper[String, UUID] =
    TypeMapper(UUID.fromString)(_.toString)

  implicit val roleTMap: TypeMapper[String, Role] =
    TypeMapper[String, Role](parse(_).flatMap(_.as[Role]).right.get)(_.asJson.toString)
}
