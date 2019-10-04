/*
 * Copyright 2014–2019 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.plugin.postgres.datasource

import slamdata.Predef._

import argonaut._, Argonaut._

import cats.effect._
import cats.implicits._

import quasar.EffectfulQSpec
import quasar.api.datasource.DatasourceError
import quasar.connector.ResourceError
import quasar.contrib.scalaz.MonadError_

import scala.concurrent.ExecutionContext.Implicits.global

import shims._

object PostgresDatasourceModuleSpec extends EffectfulQSpec[IO] {

  implicit val ioContextShift: ContextShift[IO] =
    IO.contextShift(global)

  implicit val ioTimer: Timer[IO] =
    IO.timer(global)

  implicit val ioMonadResourceErr: MonadError_[IO, ResourceError] =
    MonadError_.facet[IO](ResourceError.throwableP)

  "initialization" should {
    "fail with malformed config when not decodable" >>* {
      val cfg = Json("malformed" := true)

      PostgresDatasourceModule.lightweightDatasource[IO](cfg) use {
        case Left(DatasourceError.MalformedConfiguration(_, c, _)) =>
          IO.pure(c must_=== jString(Redacted))

        case _ => ko("Expected a malformed configuration").pure[IO]
      }
    }

    "fail when unable to connect to database" >>* {
      val cfg = Json("connectionUri" := "postgresql://localhost:1234/foobar")

      PostgresDatasourceModule.lightweightDatasource[IO](cfg) use {
        case Left(DatasourceError.ConnectionFailed(_, c, _)) =>
          IO.pure(c must_=== cfg)

        case _ => ko("Expected a connection failed").pure[IO]
      }
    }
  }
}
