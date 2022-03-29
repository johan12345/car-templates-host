/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.libraries.templates.host.internal

import android.content.ComponentName
import android.content.Context
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.logging.TelemetryEvent
import com.android.car.libraries.apphost.logging.TelemetryHandler
import com.android.car.libraries.templates.host.di.TelemetryHandlerFactory

/**
 * Holds static telemetry logging methods for common usage in the host. These methods should only be
 * used by service level component, e.g. [ClusterIconContentProvider]. Car app level logging should
 * use [TelemetryHandler] from TemplateContext
 */
class LogUtil(private val telemetryHandler: TelemetryHandler) {

  companion object {
    private lateinit var instance: LogUtil

    fun init(telemetryHandlerFactory: TelemetryHandlerFactory, applicationContext: Context?) {
      checkNotNull(applicationContext)
      instance =
        LogUtil(
          telemetryHandlerFactory.create(
            applicationContext,
            ComponentName(applicationContext, LogUtil::class.java)
          )
        )
    }

    fun log(uiAction: TelemetryEvent.UiAction) {
      log(TelemetryEvent.newBuilder(uiAction))
    }

    private fun log(builder: TelemetryEvent.Builder) {
      if (!this::instance.isInitialized) {
        L.d(LogTags.APP_HOST) { "CommonLogger is not initialized" }
        return
      }
      instance.telemetryHandler.logCarAppTelemetry(builder)
    }
  }
}
