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
import android.content.Intent
import androidx.annotation.StyleableRes
import androidx.car.app.versioning.CarAppApiLevels
import com.android.car.libraries.apphost.common.CarHostConfig
import com.android.car.libraries.templates.host.R
import com.android.car.libraries.templates.host.di.FeaturesConfig
import com.android.car.libraries.templates.host.di.HostApiLevelConfig

/** Configuration options from the car host. */
class CarHostConfigImpl(
  private val context: Context,
  appName: ComponentName,
  hostApiLevelConfig: HostApiLevelConfig,
  private val featuresConfig: FeaturesConfig
) : CarHostConfig(appName) {
  private val hostMinApi: Int =
    hostApiLevelConfig.getHostMinApiLevel(CarAppApiLevels.getOldest(), appName)
  private val hostMaxApi: Int =
    hostApiLevelConfig.getHostMaxApiLevel(CarAppApiLevels.getLatest(), appName)

  override fun getHostMinApi(): Int {
    return hostMinApi
  }

  override fun getHostMaxApi(): Int {
    return hostMaxApi
  }

  override fun isButtonColorOverriddenByOEM(): Boolean {
    return getBooleanAttr(R.attr.templateActionButtonUseOemColors)
  }

  override fun getAppUnbindSeconds(): Int {
    return context.resources.getInteger(
      R.integer.app_unbind_delay_seconds
    )
  }

  override fun getHostIntentExtrasToRemove(): MutableList<String> {
    return mutableListOf()
  }

  override fun isNewTaskFlowIntent(intent: Intent?): Boolean {
    return true
  }

  override fun getPrimaryActionOrder(): Int {
    return getIntAttr(R.attr.templateActionButtonPrimaryHorizontalOrder)
  }

  override fun isClusterEnabled(): Boolean {
    return featuresConfig.isClusterActivityEnabled()
  }

  override fun isNavPanZoomEnabled(): Boolean {
    return featuresConfig.isNavPanZoomEnabled()
  }

  override fun isPoiRoutePreviewPanZoomEnabled(): Boolean {
    return featuresConfig.isPoiRoutePreviewPanZoomEnabled()
  }

  override fun isPoiContentRefreshEnabled(): Boolean {
    return featuresConfig.isPoiContentRefreshEnabled()
  }

  private fun getBooleanAttr(attr: Int): Boolean {
    @StyleableRes val themeAttrs = intArrayOf(attr)
    val ta = context.obtainStyledAttributes(themeAttrs)
    val value = ta.getBoolean(0, false)
    ta.recycle()
    return value
  }

  private fun getIntAttr(attr: Int): Int {
    @StyleableRes val themeAttrs = intArrayOf(attr)
    val ta = context.obtainStyledAttributes(themeAttrs)
    val value = ta.getInt(0, 0)
    ta.recycle()
    return value
  }
}
