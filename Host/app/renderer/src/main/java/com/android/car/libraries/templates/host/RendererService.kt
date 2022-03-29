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
package com.android.car.libraries.templates.host

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.content.res.Configuration
import android.os.Binder
import android.os.IBinder
import android.view.LayoutInflater
import androidx.car.app.HandshakeInfo
import androidx.car.app.activity.renderer.ICarAppActivity
import androidx.car.app.activity.renderer.IRendererService
import androidx.car.app.serialization.Bundleable
import androidx.car.app.versioning.CarAppApiLevels
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.car.libraries.apphost.common.HostResourceIds
import com.android.car.libraries.apphost.common.ThreadUtils
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.logging.StatusReporter
import com.android.car.libraries.apphost.view.TemplateConverterRegistry
import com.android.car.libraries.apphost.view.TemplatePresenterRegistry
import com.android.car.libraries.templates.host.di.FeaturesConfig
import com.android.car.libraries.templates.host.di.HostApiLevelConfig
import com.android.car.libraries.templates.host.di.TelemetryHandlerFactory
import com.android.car.libraries.templates.host.di.ThemeManager
import com.android.car.libraries.templates.host.di.UxreConfig
import com.android.car.libraries.templates.host.internal.CarActivityDispatcher
import com.android.car.libraries.templates.host.internal.CommonUtils
import com.android.car.libraries.templates.host.internal.LogUtil
import com.android.car.libraries.templates.host.internal.StatusManager
import com.android.car.libraries.templates.host.internal.debug.ClusterActivity
import com.android.car.libraries.templates.host.renderer.ScreenRenderer
import com.android.car.libraries.templates.host.renderer.ScreenRendererRepository
import com.android.car.libraries.templates.host.view.presenters.common.CommonTemplateConverter
import com.android.car.libraries.templates.host.view.presenters.common.CommonTemplatePresenterFactory
import com.android.car.libraries.templates.host.view.presenters.maps.MapsTemplatePresenterFactory
import com.android.car.libraries.templates.host.view.presenters.navigation.NavigationTemplatePresenterFactory
import com.android.car.ui.CarUiLayoutInflaterFactory
import dagger.hilt.android.AndroidEntryPoint
import java.io.FileDescriptor
import java.io.PrintWriter
import javax.inject.Inject

/** A service used to render content of a car app service inside a car app activity. */
@AndroidEntryPoint
class RendererService : Service() {
  // TODO(b/182486338): Migrate the inject point to TemplateView
  @Inject lateinit var mapsTemplatePresenterFactory: MapsTemplatePresenterFactory

  @Inject lateinit var hostResourceIds: HostResourceIds

  @Inject lateinit var uxreConfig: UxreConfig

  @Inject lateinit var hostApiLevelConfig: HostApiLevelConfig

  @Inject lateinit var themeManager: ThemeManager

  @Inject lateinit var telemetryHandlerFactory: TelemetryHandlerFactory
  @Inject lateinit var hostFeaturesConfig: FeaturesConfig

  /** Whether the debug overlay is active. */
  private var isDebugOverlayActive = false

  override fun onCreate() {
    super.onCreate()
    L.d(LogTags.SERVICE) { "RendererService.onCreate" }

    // This must be executed within ANR timeout (5 seconds) of the host being launched.
    setAsForeground()
    LogUtil.init(telemetryHandlerFactory, applicationContext)

    val layoutInflater = LayoutInflater.from(this.applicationContext)
    if (layoutInflater.factory2 == null) {
      layoutInflater.factory2 = CarUiLayoutInflaterFactory()
    }

    // preload some MapView rendering code to speed things up before it is actually used
    // by PlaceListMapTemplatePresenter.
    ThreadUtils.enqueueOnMain { mapsTemplatePresenterFactory.preloadMapView(this) }
    initClusterActivity()
  }

  private fun initClusterActivity() {
    val state =
      if (hostFeaturesConfig.isClusterActivityEnabled()) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      } else {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
      }
    packageManager.setComponentEnabledSetting(
      ComponentName(this, ClusterActivity::class.java),
      state,
      PackageManager.DONT_KILL_APP
    )
  }

  private fun setAsForeground() {
    val channel =
      NotificationChannel(
        CHANNEL_ID,
        application.applicationInfo.name,
        NotificationManager.IMPORTANCE_NONE
      )
    val notificationManager = NotificationManagerCompat.from(this)
    notificationManager.createNotificationChannel(channel)

    val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
    val notification =
      notificationBuilder
        .setOngoing(true)
        .setSmallIcon(application.applicationInfo.icon)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()

    startForeground(
      FOREGROUND_SERVICE_NOTIFICATION_ID,
      notification,
      FOREGROUND_SERVICE_TYPE_LOCATION
    )
  }

  override fun onBind(intent: Intent): IBinder {
    registerPresenters()
    return RendererServiceBinder(this)
  }

  override fun onUnbind(intent: Intent): Boolean {
    L.d(LogTags.SERVICE) { "RendererService.onUnbind" }

    // Note that even when the RendererService is unbound. The CarAppService remains bound
    // because the car app can remain alive in the background (e.g. nav apps sending TBT
    // instructions). Hence we do not clear the Carhosts here.
    ScreenRendererRepository.clear()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    L.d(LogTags.SERVICE) { "RendererService.onDestroy" }

    // Note that even when the RendererService is unbound. The CarAppService remains bound
    // because the car app can remain alive in the background (e.g. nav apps sending TBT
    // instructions). Hence we do not clear the Carhosts here.
    ScreenRendererRepository.clear()
    super.onDestroy()
  }

  override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
    if (args?.contains("debug_overlay") == true) {
      if (!CommonUtils.isDebugEnabled(/* context= */ this)) {
        writer?.println("Debug enabled required for debug overlay")
        return
      }
      isDebugOverlayActive = !isDebugOverlayActive
      ScreenRendererRepository.getAll().forEach { it.showDebugOverlay(isDebugOverlayActive) }
      if (isDebugOverlayActive) {
        writer?.println("Debug overlay enabled")
      } else {
        writer?.println("Debug overlay disabled")
      }
    } else {
      writer?.let { StatusManager.reportStatus(writer, StatusReporter.Pii.HIDE) }
    }
  }

  override fun onConfigurationChanged(config: Configuration) {
    super.onConfigurationChanged(config)

    ScreenRendererRepository.getAll().forEach { it.onConfigurationChanged(config) }
  }

  private fun registerPresenters() {
    TemplatePresenterRegistry.get().clear()
    TemplatePresenterRegistry.get().register(NavigationTemplatePresenterFactory.get())
    TemplatePresenterRegistry.get().register(CommonTemplatePresenterFactory.get())
    TemplateConverterRegistry.get().register(CommonTemplateConverter.get())
    TemplatePresenterRegistry.get().register(mapsTemplatePresenterFactory)
  }

  private inner class RendererServiceBinder(val context: Context) :
    IRendererService.Stub(), CarActivityDispatcher.Callback {

    override fun initialize(
      carActivity: ICarAppActivity,
      serviceName: ComponentName,
      displayId: Int
    ): Boolean {
      L.d(LogTags.SERVICE) { "RendererServiceBinder.initialize: $serviceName" }
      val renderer = findRenderer(serviceName, displayId) ?: return false
      ThreadUtils.runOnMain { renderer.onCreateActivity(carActivity) }
      return true
    }

    override fun terminate(serviceName: ComponentName) {
      if (!isValid(serviceName)) return
      L.d(LogTags.SERVICE) { "RendererServiceBinder.terminate: $serviceName" }
      doTerminate(serviceName)
    }

    override fun onDisconnect(serviceName: ComponentName) {
      L.d(LogTags.SERVICE) { "RendererServiceBinder.onDisconnect: $serviceName" }
      doTerminate(serviceName)
    }

    private fun doTerminate(serviceName: ComponentName) {
      ThreadUtils.runOnMain { ScreenRendererRepository.remove(serviceName)?.onDestroy() }
    }

    override fun onNewIntent(intent: Intent, serviceName: ComponentName, displayId: Int): Boolean {
      L.i(LogTags.SERVICE) { "RendererServiceBinder.onNewIntent: $serviceName" }
      val renderer = findRenderer(serviceName, displayId) ?: return false
      ThreadUtils.runOnMain { renderer.onNewIntent(intent) }
      return true
    }

    override fun performHandshake(serviceName: ComponentName, appLatestApiLevel: Int): Bundleable {
      val apiLevel = Math.min(appLatestApiLevel, CarAppApiLevels.getLatest())
      L.i(LogTags.SERVICE) {
        "RendererServiceBinder.performHandshake: $serviceName, " +
          "appLatestApiLevel: $appLatestApiLevel, chosen api level: $apiLevel"
      }
      // Store in the host whenever we need to start checking versions.
      return Bundleable.create(HandshakeInfo(context.packageName, apiLevel))
    }

    private fun findRenderer(serviceName: ComponentName, displayId: Int): ScreenRenderer? {
      if (!isValid(serviceName)) return null

      return ScreenRendererRepository.computeIfAbsent(serviceName) {
        L.d(LogTags.SERVICE) {
          "RendererServiceBinder.findRenderer: $serviceName - " + "created new ScreenRenderer"
        }
        ScreenRenderer(
          context.applicationContext,
          serviceName,
          displayId,
          this,
          hostResourceIds,
          uxreConfig,
          hostApiLevelConfig,
          themeManager,
          telemetryHandlerFactory.create(context, serviceName),
          hostFeaturesConfig,
          isDebugOverlayActive
        )
      }
    }

    private fun isValid(serviceName: ComponentName?): Boolean {
      if (serviceName == null) {
        L.e(LogTags.SERVICE) { "Service name was not specified!" }
        return false
      }
      val senderPackage = context.packageManager.getNameForUid(Binder.getCallingUid())
      if (senderPackage == null || senderPackage != serviceName.packageName) {
        L.e(LogTags.SERVICE) { "Could not verify the caller!" }
        return false
      }
      return true
    }
  }

  companion object {
    const val CHANNEL_ID = "default"
    const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1
  }
}
