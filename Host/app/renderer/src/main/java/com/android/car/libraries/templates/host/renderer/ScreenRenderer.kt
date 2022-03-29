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
package com.android.car.libraries.templates.host.renderer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.VisibleForTesting
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.activity.renderer.ICarAppActivity
import androidx.car.app.activity.renderer.surface.ISurfaceListener
import androidx.car.app.activity.renderer.surface.SurfaceWrapper
import androidx.car.app.model.TemplateWrapper
import androidx.car.app.serialization.Bundleable
import androidx.car.app.utils.ThreadUtils
import com.android.car.libraries.apphost.CarHost
import com.android.car.libraries.apphost.common.BackPressedHandler
import com.android.car.libraries.apphost.common.CarAppManager
import com.android.car.libraries.apphost.common.EventManager
import com.android.car.libraries.apphost.common.HostResourceIds
import com.android.car.libraries.apphost.common.IntentUtils
import com.android.car.libraries.apphost.common.LocationMediator
import com.android.car.libraries.apphost.common.StatusBarManager
import com.android.car.libraries.apphost.common.SurfaceCallbackHandler
import com.android.car.libraries.apphost.common.TemplateContext
import com.android.car.libraries.apphost.internal.LocationMediatorImpl
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.logging.StatusReporter
import com.android.car.libraries.apphost.logging.TelemetryHandler
import com.android.car.libraries.apphost.nav.NavigationHost
import com.android.car.libraries.apphost.template.AppHost
import com.android.car.libraries.apphost.template.ConstraintHost
import com.android.car.libraries.apphost.template.UIController
import com.android.car.libraries.apphost.view.SurfaceProvider
import com.android.car.libraries.templates.host.di.FeaturesConfig
import com.android.car.libraries.templates.host.di.HostApiLevelConfig
import com.android.car.libraries.templates.host.di.ThemeManager
import com.android.car.libraries.templates.host.di.UxreConfig
import com.android.car.libraries.templates.host.internal.CarActivityDispatcher
import com.android.car.libraries.templates.host.internal.CarAppServiceInfo
import com.android.car.libraries.templates.host.internal.CarHostRepository
import com.android.car.libraries.templates.host.internal.DebugOverlayHandlerImpl
import com.android.car.libraries.templates.host.internal.InputConfigImpl
import com.android.car.libraries.templates.host.internal.InputManagerImpl
import com.android.car.libraries.templates.host.internal.InsetsListener
import com.android.car.libraries.templates.host.internal.NavigationStateCallbackImpl
import com.android.car.libraries.templates.host.internal.RendererCallback
import com.android.car.libraries.templates.host.internal.StartCarAppUtil
import com.android.car.libraries.templates.host.internal.TemplateContextImpl
import com.android.car.libraries.templates.host.view.TemplateView
import java.io.PrintWriter

/**
 * A class used to handle rendering of a single car app screen.
 *
 * <p>Once the activity is ready the [onCreateActivity] should be called to start the rendering.
 *
 * @property appName Points to the car app service which provides the data for the screen.
 * @param display The display on which the content should be displayed.
 */
class ScreenRenderer(
  private val context: Context,
  private val appName: ComponentName,
  displayId: Int,
  private val callback: CarActivityDispatcher.Callback,
  hostResourceIds: HostResourceIds,
  uxreConfig: UxreConfig,
  hostApiLevelConfig: HostApiLevelConfig,
  themeManager: ThemeManager,
  telemetryHandler: TelemetryHandler,
  featuresConfig: FeaturesConfig,
  isDebugOverlayActive: Boolean
) : BackPressedHandler, SurfaceCallbackHandler, StatusBarManager {
  private var surfaceController: SurfaceController? = null
  private lateinit var carActivity: CarActivityDispatcher
  private val carAppManager = CarAppManagerImpl()
  private val carAppServiceInfo = CarAppServiceInfo(context, appName)
  private val isNavigationApp = carAppServiceInfo.isNavigationService
  @VisibleForTesting var lastTemplate: TemplateWrapper? = null
  private val mainHandler = Handler(Looper.getMainLooper(), HandlerCallback())
  private val inputManagerListener =
    object : InputManagerImpl.InputManagerListener {
      override fun onStartInput() {
        carActivity.dispatch(ICarAppActivity::onStartInput)
      }

      override fun onStopInput() {
        carActivity.dispatch(ICarAppActivity::onStopInput)
      }

      override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int
      ) {
        carActivity.dispatchNoFail {
          it.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd)
        }
      }
    }

  private val inputManager = InputManagerImpl(inputManagerListener)

  private val inputConfig = InputConfigImpl()

  val templateContext: TemplateContext =
    TemplateContextImpl.create(
      context,
      appName,
      displayId,
      this,
      this,
      this,
      DebugOverlayHandlerImpl(isDebugOverlayActive),
      inputManager,
      inputConfig,
      carAppManager,
      isNavigationApp,
      hostResourceIds,
      uxreConfig,
      hostApiLevelConfig,
      themeManager,
      telemetryHandler,
      featuresConfig
    )

  private var templateView = TemplateView.create(templateContext)

  @VisibleForTesting
  val uiController =
    object : UIController {
      override fun getSurfaceProvider(appName: ComponentName?): SurfaceProvider {
        return templateView.surfaceProvider
      }

      override fun setTemplate(appName: ComponentName?, template: TemplateWrapper?) {
        mainHandler.removeMessages(MSG_SET_TEMPLATE)
        val msg = mainHandler.obtainMessage(MSG_SET_TEMPLATE)
        msg.obj = template

        mainHandler.sendMessage(msg)
      }
    }

  init {
    val locationMediator =
      LocationMediatorImpl.create(templateContext.eventManager) { enable: Boolean ->
        trySetEnableAppLocationUpdates(enable)
      }
    templateContext.registerAppHostService(LocationMediator::class.java, locationMediator)
  }

  override fun onBackPressed() {
    val carHost = CarHostRepository.get(appName)
    val appHost = carHost?.getHostOrThrow(CarContext.APP_SERVICE) as? AppHost
    appHost?.onBackPressed()
  }

  override fun onScroll(distanceX: Float, distanceY: Float) {
    val carHost = CarHostRepository.get(appName)
    val appHost = carHost?.getHostOrThrow(CarContext.APP_SERVICE) as? AppHost
    appHost?.onSurfaceScroll(distanceX, distanceY)
  }

  override fun onFling(velocityX: Float, velocityY: Float) {
    val carHost = CarHostRepository.get(appName)
    val appHost = carHost?.getHostOrThrow(CarContext.APP_SERVICE) as? AppHost
    appHost?.onSurfaceFling(velocityX, velocityY)
  }

  override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
    val carHost = CarHostRepository.get(appName)
    val appHost = carHost?.getHostOrThrow(CarContext.APP_SERVICE) as? AppHost
    appHost?.onSurfaceScale(focusX, focusY, scaleFactor)
  }

  override fun setStatusBarState(
    statusBarState: StatusBarManager.StatusBarState?,
    rootView: View?
  ) {
    // TODO: Not yet implemented
    Log.v(
      LogTags.APP_HOST,
      "StatusBar state updated to $statusBarState. " + "RootView is $rootView."
    )
  }

  /** Requests to enable or disable location updates from the app. */
  private fun trySetEnableAppLocationUpdates(enabled: Boolean) {
    val carHost = CarHostRepository.get(appName)
    val appHost = carHost?.getHostOrThrow(CarContext.APP_SERVICE) as? AppHost
    appHost?.trySetEnableLocationUpdates(enabled)
  }

  private fun createBinderIntent(intent: Intent) =
    Intent().apply {
      action = CarAppService.SERVICE_INTERFACE
      component = appName
      IntentUtils.embedOriginalIntent(this, intent)
    }

  fun onCreateActivity(carActivity: ICarAppActivity) {
    this.carActivity = CarActivityDispatcher(appName, carActivity, callback)
    val carHost = CarHostRepository.computeIfAbsent(appName) { CarHost.create(templateContext) }
    carHost.registerHostService(CarContext.APP_SERVICE) { appBinding ->
      AppHost.create(uiController, appBinding, templateContext)
    }
    carHost.registerHostService(CarContext.CONSTRAINT_SERVICE) {
      ConstraintHost.create(templateContext)
    }

    // Register the navigation host service only if the app is a navigation app. An
    // exception will be thrown if non-nav apps try to request access to the
    // navigation host service.
    if (templateContext.carAppPackageInfo.isNavigationApp) {
      L.d(LogTags.NAVIGATION, "Registering navigation service")
      carHost.registerHostService(CarContext.NAVIGATION_SERVICE) { appBinding: Any? ->
        NavigationHost.create(
          appBinding,
          templateContext,
          NavigationStateCallbackImpl.create(templateContext)
        )
      }
    }

    // Before returning the CarHost instance, check that the AppHost service still has a
    // reference to the UiController instance of this activity, and if not, update it.
    // This could happen if the activity is destroyed and re-created after, while the
    // CarAppService binding remains alive through those changes.
    val appHost: AppHost = carHost.getHostOrThrow(CarContext.APP_SERVICE) as AppHost
    if (uiController != appHost.getUIController()) {
      L.d(
        LogTags.APP_HOST,
        "Activity has been re-created, updating UI controller and " +
          "template context in the host services"
      )
      appHost.setUIController(uiController)
      carHost.setTemplateContext(templateContext)
    }

    templateView.setParentLifecycle(carHost.lifecycle)
    templateView.setTemplateContext(templateContext)

    val surfaceListener = surfaceListener(carActivity, carHost)
    carActivity.setSurfaceListener(surfaceListener)

    templateContext.eventManager.subscribeEvent(this, EventManager.EventType.CONSTRAINTS) {
      reloadTemplate()
    }
  }

  fun onNewIntent(intent: Intent) {
    val binderIntent = createBinderIntent(intent)
    CarHostRepository.get(appName)?.bindToApp(binderIntent)
  }

  /** Updates the context with given configuration. */
  fun onConfigurationChanged(config: Configuration) {
    templateContext.updateConfiguration(config)
  }

  /**
   * Called when the activity has disconnected from the renderer service. This instance shouldn't be
   * used again after this point.
   */
  fun onDestroy() {
    L.d(
      LogTags.APP_HOST,
      "Activity disconnected from the renderer service. " +
        "Destroying its associated screen renderer."
    )
  }

  fun reportStatus(pw: PrintWriter, piiHandling: StatusReporter.Pii) {
    surfaceController?.reportStatus(pw, piiHandling)
    pw.printf("- last template: %s\n", lastTemplate)
  }

  /** Shows/hides debug overlay if isVisible is {@code true}/{@code false} respectively. */
  fun showDebugOverlay(isVisible: Boolean) {
    templateContext.debugOverlayHandler.isActive = isVisible
  }

  private fun surfaceListener(carActivity: ICarAppActivity, carHost: CarHost): ISurfaceListener {
    return object : ISurfaceListener.Stub() {
      override fun onSurfaceAvailable(surfaceWrapperBundleable: Bundleable) {
        val surfaceWrapper = surfaceWrapperBundleable.get()
        if (surfaceWrapper !is SurfaceWrapper) {
          Log.e(
            LogTags.APP_HOST,
            "onSurfaceAvailable event invoked with unexpected type: $surfaceWrapper"
          )
          // TODO(b/181775931): Better handle error case
          return
        }

        val width = surfaceWrapper.width
        val height = surfaceWrapper.height
        ThreadUtils.runOnMain {
          val surfaceController = getOrCreateSurfaceController(surfaceWrapper)
          surfaceController.setView(templateView, width, height)
          this@ScreenRenderer.surfaceController = surfaceController
          val surfacePackage = surfaceController.obtainSurfacePackage()
          val rendererCallback = RendererCallback(carHost, inputManager)
          val insetsListener = InsetsListener(templateView)

          try {
            carActivity.setInsetsListener(insetsListener)
            carActivity.setSurfacePackage(surfacePackage)
            carActivity.registerRendererCallback(rendererCallback)
          } catch (e: RemoteException) {
            Log.e(LogTags.APP_HOST, "Binder invocation failed", e)
            // TODO(b/181775931): Better handle error case
          }

          surfaceController.releaseSurfacePackage(surfacePackage)
        }
      }

      override fun onSurfaceChanged(surfaceWrapperBundleable: Bundleable) {
        val surfaceWrapper = surfaceWrapperBundleable.get()
        if (surfaceWrapper !is SurfaceWrapper) {
          Log.e(
            LogTags.APP_HOST,
            "onSurfaceChanged event invoked with unexpected type: $surfaceWrapper"
          )
          return
        }

        ThreadUtils.runOnMain { surfaceController?.relayout(surfaceWrapper) }
      }

      override fun onSurfaceDestroyed(surfaceWrapperBundleable: Bundleable) {
        val surfaceWrapper = surfaceWrapperBundleable.get()
        if (surfaceWrapper !is SurfaceWrapper) {
          Log.e(
            LogTags.APP_HOST,
            "onSurfaceDestroyed event invoked with unexpected type: $surfaceWrapper"
          )
          return
        }

        ThreadUtils.runOnMain { surfaceController?.releaseSurface() }
      }
    }
  }

  private fun getOrCreateSurfaceController(surfaceWrapper: SurfaceWrapper): SurfaceController {
    return if (SUPPORTS_SURFACE_VIEW_HOST_WRAPPER && surfaceWrapper.hostToken != null) {
      SurfaceControlViewHostController(context, surfaceWrapper)
    } else {
      // Reuse old instance for SDK < 30 to avoid flicker (b/187841390)
      surfaceController
        ?: LegacySurfaceController(context, templateContext) { e ->
          Log.e(LogTags.APP_HOST, "LegacySurfaceController error", e)
          carActivity.disconnect()
        }
    }
  }

  private fun reloadTemplate() {
    ThreadUtils.runOnMain { lastTemplate?.let { templateView.setTemplate(it) } }
  }

  private inner class CarAppManagerImpl : CarAppManager {
    override fun startCarApp(intent: Intent) {
      StartCarAppUtil.validateStartCarAppIntent(
        context,
        appName.packageName,
        intent,
        isNavigationApp
      )
      carActivity.dispatch { it.startCarApp(intent) }
    }

    override fun finishCarApp() {
      carActivity.dispatch { it.finishCarApp() }
      ThreadUtils.runOnMain { CarHostRepository.remove(appName) }
    }
  }

  companion object {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    private val SUPPORTS_SURFACE_VIEW_HOST_WRAPPER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    private const val MSG_SET_TEMPLATE = 1
  }

  /** A [Handler.Callback] used to process the message queue for the ui controller. */
  private inner class HandlerCallback : Handler.Callback {
    private var lastUpdateUptimeMillis = -Long.MAX_VALUE

    override fun handleMessage(msg: Message): Boolean {
      if (msg.what == MSG_SET_TEMPLATE) {
        // Use SystemClock.uptimeMillis since that is what Handler uses for time.
        val currentUptimeMillis: Long = SystemClock.uptimeMillis()
        val updateUptimeMillis: Long = lastUpdateUptimeMillis + 1000
        if (updateUptimeMillis > currentUptimeMillis) {
          val message: Message = mainHandler.obtainMessage(MSG_SET_TEMPLATE)
          message.obj = msg.obj
          mainHandler.removeMessages(MSG_SET_TEMPLATE)
          mainHandler.sendMessageAtTime(message, updateUptimeMillis)
          return true
        }
        lastUpdateUptimeMillis = currentUptimeMillis
        val template: TemplateWrapper = msg.obj as TemplateWrapper

        lastTemplate = template
        templateView.setTemplate(template)
        return true
      } else {
        L.w(LogTags.APP_HOST, "Unknown message: %s", msg)
      }
      return false
    }
  }
}
