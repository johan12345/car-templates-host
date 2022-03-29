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

import android.car.Car
import android.car.CarAppFocusManager
import android.car.CarAppFocusManager.APP_FOCUS_TYPE_NAVIGATION as APP_TYPE_NAVIGATION
import android.car.CarAppFocusManager.OnAppFocusOwnershipCallback as FocusCallback
import android.car.cluster.navigation.NavigationState
import android.car.navigation.CarNavigationStatusManager
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.car.app.navigation.model.Trip
import androidx.core.os.bundleOf
import com.android.car.libraries.apphost.common.CarAppPackageInfo
import com.android.car.libraries.apphost.common.TemplateContext
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.android.car.libraries.templates.host.R


/**
 * Coordinate navigation and focus control between all Template Apps. Apps can start sending
 * navigation events after requesting focus, but it's possible some events will be dropped while
 * focus is being obtained.
 */
class NavigationCoordinator
private constructor(
  carAppFocusManagerProvider: () -> CarAppFocusManager?,
  carNavStatusManagerProvider: () -> CarNavigationStatusManager?,
  private val shouldShareNavState: Boolean
) : FocusCallback {
  /**
   * Instances of this interface will be compared against each other for equality. Either make sure
   * you are sending the same instance per app, or implement [equals] to account for this.
   */
  interface NavAppFocusOwner {
    val packageInfo: CarAppPackageInfo
    fun onFocusLost()
  }

  private val focusManager: CarAppFocusManager? by lazy(carAppFocusManagerProvider)
  private val navigationManager: CarNavigationStatusManager? by lazy(carNavStatusManagerProvider)

  private val _navigationState = MutableStateFlow<HostNavState>(HostNavState.NotNavigating)
  val navigationState = _navigationState.asStateFlow()

  /** Whether or not the Host has navigation focus currently */
  private val isOwningFocus = AtomicBoolean(false)
  private var currentNavApp: NavAppFocusOwner? = null

  /**
   * Apps must request Focus (see [requestAppFocus]) before sending navigation events. Also, there's
   * a chance that some events will be dropped on the floor while focus is being obtained.
   */
  fun sendNavigationStateChange(
    navApp: NavAppFocusOwner,
    // TODO(b/206694446): Only accept Trip and do the Proto conversion here.
    navigationState: NavigationState.NavigationStateProto,
    templateContext: TemplateContext,
    trip: Trip? = null
  ) =
    synchronized(this) {
      if (isFocused(navApp)) {
        _navigationState.value =
          if (trip != null) HostNavState.Navigating(trip, templateContext, navApp.packageInfo)
          else HostNavState.NotNavigating
        if (shouldShareNavState) {
          navigationManager?.sendNavigationStateChange(navigationState.asBundle())
        }
      } else {
        L.w(LogTags.NAVIGATION) {
          val packageName = navApp.packageInfo.componentName.packageName
          "Package $packageName is trying to send NavigationState updates without owning focus"
        }
      }
    }

  /**
   * Note that a result of [CarAppFocusManager.APP_FOCUS_REQUEST_SUCCEEDED] does not mean you have
   * focus yet. This call is asynchronous and
   * [CarAppFocusManager.OnAppFocusOwnershipCallback.onAppFocusOwnershipGranted] will be called when
   * focus is granted for the app.
   */
  fun requestAppFocus(navApp: NavAppFocusOwner) =
    synchronized(this) {
      val focusManager =
        focusManager
          ?: run {
            L.w(LogTags.NAVIGATION) {
              "Couldn't obtain focusManager. Are you missing a permission?"
            }
            navApp.onFocusLost()
            return
          }

      if (navApp != currentNavApp) {
        currentNavApp?.onFocusLost()
        currentNavApp = navApp
      }

      if (!isOwningFocus.get()) {
        // request focus from system
        val result = focusManager.requestAppFocus(APP_TYPE_NAVIGATION, this)
        if (result == CarAppFocusManager.APP_FOCUS_REQUEST_FAILED) {
          onAppFocusOwnershipLost(APP_TYPE_NAVIGATION)
        }
      } else {
        clearNavState()
      }
    }

  /** Notify that [navApp] is done navigation and no longer requires focus. */
  fun abandonAppFocus(navApp: NavAppFocusOwner) =
    synchronized(this) {
      if (isFocused(navApp)) {
        onAppFocusOwnershipLost(APP_TYPE_NAVIGATION)
        focusManager?.abandonAppFocus(this, APP_TYPE_NAVIGATION)
        _navigationState.value = HostNavState.NotNavigating
      }
    }

  override fun onAppFocusOwnershipLost(appType: Int) =
    synchronized(this) {
      L.d(LogTags.NAVIGATION) { "Host focus Lost" }
      isOwningFocus.set(false)
      currentNavApp?.onFocusLost()
      currentNavApp = null
    }

  override fun onAppFocusOwnershipGranted(appType: Int) =
    synchronized(this) {
      L.d(LogTags.NAVIGATION) { "Host focus granted" }
      isOwningFocus.set(true)
      if (!shouldShareNavState) {
        L.d(LogTags.NAVIGATION, "NavState data will not sent to system.")
        clearNavState()
      }
    }

  private fun clearNavState() {
    val emptyNavState = NavigationState.NavigationStateProto.getDefaultInstance()
    navigationManager?.sendNavigationStateChange(emptyNavState.asBundle())
  }

  /** returns whether or not [navApp] has focus currently */
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun isFocused(navApp: NavAppFocusOwner): Boolean =
    synchronized(this) { isOwningFocus.get() && this.currentNavApp == navApp }

  companion object {
    private lateinit var instance: NavigationCoordinator

    fun getInstance(context: Context): NavigationCoordinator {
      if (!this::instance.isInitialized) {
        val themeAttrs = intArrayOf(R.attr.templateSendNavStateToSystem)
        val ta = context.obtainStyledAttributes(themeAttrs)
        val shouldShareNavState =
          ta.getBoolean(0, context.resources.getBoolean(R.bool.send_navstates_to_system))
        ta.recycle()
        instance =
          NavigationCoordinator(
            carAppFocusManagerProvider = { context.getCarService(Car.APP_FOCUS_SERVICE) },
            carNavStatusManagerProvider = { context.getCarService(Car.CAR_NAVIGATION_SERVICE) },
            shouldShareNavState
          )
      }
      return instance
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun testInstance(
      carAppFocusManager: CarAppFocusManager,
      carNavStatusManager: CarNavigationStatusManager
    ) = NavigationCoordinator({ carAppFocusManager }, { carNavStatusManager }, true)

    private inline fun <reified T> Context.getCarService(serviceName: String): T? {
      val car: Car? = Car.createCar(this)
      if (car == null) {
        L.e(LogTags.NAVIGATION) { "Nav state disabled: Unable to connect to CarService" }
        return null
      }
      return runCatching { car.getCarManager(serviceName) as T? }
        .onSuccess { L.d(LogTags.NAVIGATION) { "Obtained service: $serviceName" } }
        .onFailure {
          L.e(LogTags.NAVIGATION, it) {
            "Nav state disabled: Unable to obtain access to $serviceName."
          }
        }
        .getOrNull()
    }
  }
}

private const val NAVIGATION_STATE_PROTO_BUNDLE_KEY = "navstate2"

private fun NavigationState.NavigationStateProto.asBundle() =
  bundleOf(NAVIGATION_STATE_PROTO_BUNDLE_KEY to this.toByteArray())

sealed class HostNavState {
  object NotNavigating : HostNavState()
  class Navigating(
    val trip: Trip,
    val templateContext: TemplateContext,
    val packageInfo: CarAppPackageInfo
  ) : HostNavState()
}
