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

import android.car.cluster.navigation.NavigationState
import androidx.car.app.navigation.model.Trip
import com.android.car.libraries.apphost.common.CarAppPackageInfo
import com.android.car.libraries.apphost.common.TemplateContext
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.nav.NavigationHost
import com.android.car.libraries.apphost.nav.NavigationStateCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.android.car.libraries.templates.host.R

/** Handles navigation state change events from [NavigationHost]. */
class NavigationStateCallbackImpl
private constructor(
  private val templateContext: TemplateContext,
  private val navigationStateConverter: NavigationStateConverter
) : NavigationStateCallback {
  private var onNavigationStopRunnable: Runnable? = null

  private val packageInfo = templateContext.carAppPackageInfo
  private val navigationCoordinator by lazy { NavigationCoordinator.getInstance(templateContext) }

  private val navApp =
    object : NavigationCoordinator.NavAppFocusOwner {
      override val packageInfo: CarAppPackageInfo
        get() = templateContext.carAppPackageInfo

      override fun onFocusLost() {
        onNavigationStopRunnable?.run()
      }
    }

  override fun onUpdateTrip(trip: Trip): Boolean {
    L.v(LogTags.NAVIGATION) { "onUpdateTrip $packageInfo" }
    CoroutineScope(Dispatchers.Default).launch {
      // Conversion shouldn't take a long time. If it hangs for too long, just kill it.
      val timeMillis =
        templateContext
          .resources
          .getInteger(R.integer.cluster_trip_to_navstate_conversion_timeout_millis)
          .toLong()
      val navigationState =
        withTimeout(timeMillis) { navigationStateConverter.tripToNavigationState(trip) }
      navigationCoordinator.sendNavigationStateChange(
        navApp,
        navigationState,
        templateContext,
        trip
      )
    }
    return true
  }

  override fun onNavigationStarted(onNavigationStopRunnable: Runnable) {
    L.v(LogTags.NAVIGATION) { "onNavigationStarted ${templateContext.carAppPackageInfo}" }

    this.onNavigationStopRunnable = onNavigationStopRunnable
    if (templateContext.carHostConfig.isClusterEnabled) {
      navigationCoordinator.requestAppFocus(navApp)
    }
  }

  override fun onNavigationEnded() {
    L.v(LogTags.NAVIGATION) { "onNavigationEnded ${templateContext.carAppPackageInfo}" }

    // Remove directions from cluster
    navigationCoordinator.sendNavigationStateChange(
      navApp,
      NavigationState.NavigationStateProto.getDefaultInstance(),
      templateContext
    )

    navigationCoordinator.abandonAppFocus(navApp)

    onNavigationStopRunnable = null
  }

  companion object {
    fun create(templateContext: TemplateContext): NavigationStateCallback {
      return NavigationStateCallbackImpl(
        templateContext,
        NavigationStateConverterImpl(templateContext)
      )
    }
  }
}
