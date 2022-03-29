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
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import com.android.car.libraries.apphost.common.ANRHandler
import com.android.car.libraries.apphost.common.AppBindingStateProvider
import com.android.car.libraries.apphost.common.AppDispatcher
import com.android.car.libraries.apphost.common.BackPressedHandler
import com.android.car.libraries.apphost.common.CarAppError
import com.android.car.libraries.apphost.common.CarAppManager
import com.android.car.libraries.apphost.common.CarAppPackageInfo
import com.android.car.libraries.apphost.common.CarHostConfig
import com.android.car.libraries.apphost.common.ColorContrastCheckState
import com.android.car.libraries.apphost.common.ColorUtils
import com.android.car.libraries.apphost.common.DebugOverlayHandler
import com.android.car.libraries.apphost.common.ErrorHandler
import com.android.car.libraries.apphost.common.EventManager
import com.android.car.libraries.apphost.common.EventManager.EventType
import com.android.car.libraries.apphost.common.HostResourceIds
import com.android.car.libraries.apphost.common.RoutingInfoState
import com.android.car.libraries.apphost.common.StatusBarManager
import com.android.car.libraries.apphost.common.SurfaceCallbackHandler
import com.android.car.libraries.apphost.common.SurfaceInfoProvider
import com.android.car.libraries.apphost.common.SystemClockWrapper
import com.android.car.libraries.apphost.common.TemplateContext
import com.android.car.libraries.apphost.common.ToastController
import com.android.car.libraries.apphost.distraction.constraints.ConstraintsProvider
import com.android.car.libraries.apphost.input.InputConfig
import com.android.car.libraries.apphost.input.InputManager
import com.android.car.libraries.apphost.internal.ANRHandlerImpl
import com.android.car.libraries.apphost.internal.AppDispatcherImpl
import com.android.car.libraries.apphost.internal.CarAppPackageInfoImpl
import com.android.car.libraries.apphost.logging.TelemetryHandler
import com.android.car.libraries.templates.host.di.FeaturesConfig
import com.android.car.libraries.templates.host.di.HostApiLevelConfig
import com.android.car.libraries.templates.host.di.ThemeManager
import com.android.car.libraries.templates.host.di.UxreConfig
import java.io.PrintWriter

/** A [TemplateContext] to provide to hosts and presenters. */
class TemplateContextImpl
private constructor(
  context: Context,
  appName: ComponentName,
  private val backPressedHandler: BackPressedHandler,
  private val surfaceCallbackHandler: SurfaceCallbackHandler,
  private val statusBarManager: StatusBarManager,
  errorHandler: ErrorHandler,
  private val toastController: ToastController,
  private val displayId: Int,
  private val inputManager: InputManager,
  private val inputConfig: InputConfig,
  private val carAppManager: CarAppManager,
  private val telemetryHandler: TelemetryHandler,
  private val debugOverlayHandler: DebugOverlayHandler,
  private val routingInfoState: RoutingInfoState,
  private val colorContrastCheckState: ColorContrastCheckState,
  private val carHostConfig: CarHostConfig,
  private val systemClockWrapper: SystemClockWrapper,
  isNavigationApp: Boolean,
  private val hostResourceIds: HostResourceIds,
  uxreConfig: UxreConfig,
  themeManager: ThemeManager
) : TemplateContext(context) {

  private val carAppPackageInfo: CarAppPackageInfo =
    CarAppPackageInfoImpl.create(
      context,
      appName,
      isNavigationApp,
      hostResourceIds,
      AppIconLoaderImpl
    )
  private val eventManager: EventManager
  private val surfaceInfoProvider: SurfaceInfoProvider
  private val appDispatcher: AppDispatcher
  private var appConfigurationContext: Context? = null
  private var errorHandler: ErrorHandler
  private val anrHandler: ANRHandler
  private val constraintsProvider: ConstraintsProvider
  private var lastError: CarAppError? = null
  private val appBindingStateProvider: AppBindingStateProvider

  init {
    this.errorHandler =
      ErrorHandler { error ->
        lastError = error
        errorHandler.showError(error)
      }

    themeManager.applyTheme(context)

    appBindingStateProvider = AppBindingStateProvider()
    eventManager = EventManager()
    anrHandler = ANRHandlerImpl.create(appName, errorHandler, telemetryHandler, eventManager)
    constraintsProvider = ConstraintsProviderImpl(context, eventManager, uxreConfig)
    surfaceInfoProvider = SurfaceInfoProviderImpl(eventManager)
    appDispatcher =
      AppDispatcherImpl.create(
        appName,
        errorHandler,
        anrHandler,
        telemetryHandler,
        appBindingStateProvider
      )

    // Create a context configured with this context's configuration, the car display's display
    // metrics, and the remote app's theme.
    val packageContext = ColorUtils.getPackageContext(context, appName.packageName)
    if (packageContext == null) {
      appConfigurationContext = null
    } else {
      val configuration = resources.configuration
      val display = context.getSystemService(DisplayManager::class.java).getDisplay(displayId)
      appConfigurationContext =
        packageContext.createDisplayContext(display).createConfigurationContext(configuration)
      appConfigurationContext?.setTheme(ColorUtils.loadThemeId(context, appName))
    }
  }

  override fun getErrorHandler() = errorHandler
  override fun getAppConfigurationContext() = appConfigurationContext
  override fun getStatusBarManager() = statusBarManager
  override fun getInputManager() = inputManager
  override fun getInputConfig() = inputConfig
  override fun getCarAppPackageInfo() = carAppPackageInfo
  override fun getBackPressedHandler() = backPressedHandler
  override fun getSurfaceCallbackHandler() = surfaceCallbackHandler
  override fun getSurfaceInfoProvider() = surfaceInfoProvider
  override fun getEventManager() = eventManager
  override fun getAnrHandler() = anrHandler
  override fun getAppDispatcher() = appDispatcher
  override fun getToastController() = toastController
  override fun getCarAppManager() = carAppManager
  override fun getTelemetryHandler() = telemetryHandler
  override fun getDebugOverlayHandler() = debugOverlayHandler
  override fun getHostResourceIds() = hostResourceIds
  override fun getRoutingInfoState() = routingInfoState
  override fun getColorContrastCheckState() = colorContrastCheckState
  override fun getConstraintsProvider() = constraintsProvider
  override fun getCarHostConfig() = carHostConfig
  override fun getSystemClockWrapper() = systemClockWrapper
  override fun getAppBindingStateProvider() = appBindingStateProvider

  override fun updateConfiguration(configuration: Configuration?) {
    appConfigurationContext =
      configuration?.let { appConfigurationContext?.createConfigurationContext(configuration) }

    // Propagate the configuration changed event to any listeners.
    getEventManager().dispatchEvent(EventType.CONFIGURATION_CHANGED)
  }

  override fun reportStatus(pw: PrintWriter) {
    pw.printf("- app package info: %s\n", carAppPackageInfo)
    pw.printf("- last error: %s\n", if (lastError != null) lastError else "n/a")
  }

  companion object {
    /** Creates a [TemplateContextImpl] for the car app identified by the given [ComponentName] */
    fun create(
      context: Context,
      appName: ComponentName,
      displayId: Int,
      backPressedHandler: BackPressedHandler,
      surfaceCallbackHandler: SurfaceCallbackHandler,
      statusBarManager: StatusBarManager,
      debugOverlayHandler: DebugOverlayHandler,
      inputManager: InputManager,
      inputConfig: InputConfig,
      carAppManager: CarAppManager,
      isNavigationApp: Boolean,
      hostResourceIds: HostResourceIds,
      uxreConfig: UxreConfig,
      hostApiLevelConfig: HostApiLevelConfig,
      themeManager: ThemeManager,
      telemetryHandler: TelemetryHandler,
      featuresConfig: FeaturesConfig
    ): TemplateContextImpl {
      return TemplateContextImpl(
        context,
        appName,
        backPressedHandler,
        surfaceCallbackHandler,
        statusBarManager,
        ErrorHandlerImpl.create(context, appName, carAppManager, hostResourceIds),
        ToastControllerImpl(context),
        displayId,
        inputManager,
        inputConfig,
        carAppManager,
        telemetryHandler,
        debugOverlayHandler,
        RoutingInfoStateImpl(),
        ColorContrastCheckStateImpl(),
        CarHostConfigImpl(context, appName, hostApiLevelConfig, featuresConfig),
        SystemClockWrapper(),
        isNavigationApp,
        hostResourceIds,
        uxreConfig,
        themeManager
      )
    }
  }
}
