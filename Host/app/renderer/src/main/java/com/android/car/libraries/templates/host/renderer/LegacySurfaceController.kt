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

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceControlViewHost
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.car.app.activity.renderer.surface.LegacySurfacePackage
import androidx.car.app.activity.renderer.surface.SurfaceControlCallback
import androidx.car.app.activity.renderer.surface.SurfaceWrapper
import androidx.car.app.serialization.Bundleable
import androidx.car.app.utils.ThreadUtils
import com.android.car.libraries.apphost.common.TemplateContext
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.logging.StatusReporter
import java.io.PrintWriter
import java.util.function.Consumer

/**
 * A presenter similar to [SurfaceControlViewHost] that conforms to [SurfaceController].
 *
 * <p>This presenter should only be used if API version is lower than [Build.VERSION_CODES.R].
 * Otherwise, [SurfaceControlViewHostController] should be used.
 */
class LegacySurfaceController(
  private val context: Context,
  private val templateContext: TemplateContext,
  private val errorHandler: Consumer<Throwable>
) : SurfaceController {

  private var presentation: Presentation? = null
  private var virtualDisplay: VirtualDisplay? = null
    set(value) {
      field?.release()?.also { Log.d(LogTags.APP_HOST, "Released old Display") }
      field = value
    }
  private var width: Int = 0
  private var height: Int = 0
  private var densityDpi: Int = 0
  private var contentView: View? = null

  /** An interface for listening to key events. */
  // TODO(b/192397819): Remove once SurfaceControlCallback supports the interface.
  interface OnKeyListener {
    /** Notifies the key event. */
    fun onKeyEvent(event: KeyEvent)
  }

  private val surfaceControl =
    object : SurfaceControlCallback, OnKeyListener {
      override fun setSurfaceWrapper(surfaceWrapper: SurfaceWrapper) {
        ThreadUtils.runOnMain {
          // Since {@link SurfaceHolder.Callback} gives a guarantee that
          // {@link SurfaceHolder.Callback#surfaceChanged} "is always called at least once, after"
          // {@link SurfaceHolder.Callback#surfaceCreated}, we should only call
          // {@link #updatePresentation} if the library is not adjusting insets. This will prevent
          // two virtual displays from being created with back-to-back calls of
          // {@link #setSurfaceWrapper} and {@link #relayout} when library is adjusting insets.
          if (!libraryAdjustsInsets(templateContext.carHostConfig.appInfo?.libraryDisplayVersion)) {
            Log.d(
              LogTags.APP_HOST,
              "SetSurfaceWrapper: " + "(${surfaceWrapper.width} x ${surfaceWrapper.height})"
            )
            updatePresentation(surfaceWrapper)
          }
        }
      }

      override fun onError(msg: String, e: Throwable) {
        Log.e(LogTags.APP_HOST, msg, e)
        errorHandler.accept(e)
      }

      override fun onWindowFocusChanged(hasFocus: Boolean, isInTouchMode: Boolean) {
        ThreadUtils.runOnMain {
          if (contentView != null) {
            presentation?.window?.setLocalFocus(hasFocus, isInTouchMode)
          }
        }
      }

      override fun onTouchEvent(event: MotionEvent) {
        ThreadUtils.runOnMain { presentation?.window?.injectInputEvent(event) }
      }

      override fun onKeyEvent(event: KeyEvent) {
        ThreadUtils.runOnMain { presentation?.window?.superDispatchKeyEvent(event) }
      }
    }

  private val surfacePackage = Bundleable.create(LegacySurfacePackage(surfaceControl))

  override fun obtainSurfacePackage(): Bundleable = surfacePackage

  override fun releaseSurfacePackage(value: Bundleable) {
    // Nothing to do here. LegacySurfacePackage doesn't need to be released.
  }

  override fun releaseSurface() {
    virtualDisplay?.surface = null
  }

  // TODO(b/208313104): Remove once majority of 3p applications migrated to 1.2.0-alpha-02.
  private fun libraryAdjustsInsets(libraryDisplayVersion: String?): Boolean {
    if (libraryDisplayVersion == null ||
        libraryDisplayVersion.startsWith("1.1") ||
        libraryDisplayVersion == "1.2.0-alpha01"
    ) {
      return false
    }
    return true
  }

  override fun relayout(surfaceWrapper: SurfaceWrapper) {
    Log.i(LogTags.APP_HOST, "Relayout: " + "(${surfaceWrapper.width} x ${surfaceWrapper.height})")

    if (libraryAdjustsInsets(templateContext.carHostConfig.appInfo?.libraryDisplayVersion)) {
      Log.i(LogTags.APP_HOST, "Library does adjust insets.")
      // A size change in the surface view requires a change in the dimensions of the virtual
      // display created on top of such surface. This can only be achieved by recreating the display
      // and adjusting the presentation on top of it. For this to be efficient, insets changes
      // should be managed on the host side (see InsetsListener), in order to avoid unnecessary
      // display recreations.
      updatePresentation(surfaceWrapper)
    } else {
      Log.i(LogTags.APP_HOST, "Library does not adjust insets.")
      // When library does not adjust the insets, host gets relayout calls even when the keyboard is
      // displayed. In this case we should not recreate the presentation since that will release the
      // first responder and dismissed the keyboard. Instead we need to adjust the size of the
      // containerView.
      contentView?.layoutParams =
        FrameLayout.LayoutParams(surfaceWrapper.width, surfaceWrapper.height)
    }
  }

  override fun setView(view: View, width: Int, height: Int) {
    contentView = view
  }

  override fun reportStatus(pw: PrintWriter, piiHandling: StatusReporter.Pii) {
    pw.printf(
      "- virtual display id: %s, width: %d, height: %d, density: %d dpi\n",
      virtualDisplay?.display?.displayId ?: "-",
      contentView?.layoutParams?.width ?: 0,
      contentView?.layoutParams?.height ?: 0,
      densityDpi
    )
  }

  private fun updatePresentation(surfaceWrapper: SurfaceWrapper) {
    if (!reuseVirtualDisplay(surfaceWrapper)) {
      setNewDisplayAndPresentation(surfaceWrapper)
    }

    // Attach contentView to Presentation if it's not already there
    contentView?.takeIf { !it.isAttachedTo(presentation) }?.let { contentView ->
      Log.i(LogTags.APP_HOST, "Attaching contentView to Presentation")
      (contentView.parent as ViewGroup?)?.removeView(contentView)
      presentation?.setContentView(contentView)
      contentView.layoutParams = FrameLayout.LayoutParams(width, height)
      contentView.invalidate()
    }
  }

  /**
   * Attaches the new [Surface] to an existing [VirtualDisplay], if possible.
   *
   * @return [false] if there's no existing [VirtualDisplay], or its dimensions don't match. [true]
   * if reuse was possible.
   */
  private fun reuseVirtualDisplay(surfaceWrapper: SurfaceWrapper): Boolean {
    if (virtualDisplay != null &&
        width == surfaceWrapper.width &&
        height == surfaceWrapper.height &&
        densityDpi == surfaceWrapper.densityDpi
    ) {
      Log.i(LogTags.APP_HOST, "Reusing existing VirtualDisplay with new Surface ($width x $height)")
      virtualDisplay?.surface = surfaceWrapper.surface
      return true
    }
    return false
  }

  /**
   * Creates, stores and shows a new [VirtualDisplay] and [Presentation] for the given
   * [SurfaceWrapper].
   */
  private fun setNewDisplayAndPresentation(surfaceWrapper: SurfaceWrapper) {
    Log.i(
      LogTags.APP_HOST,
      "Creating new VirtualDisplay and Presentation " +
        "(${surfaceWrapper.width} x ${surfaceWrapper.height})"
    )
    val displayManager = context.getSystemService(DisplayManager::class.java)
    virtualDisplay =
      displayManager.createVirtualDisplay(
        VIRTUAL_DISPLAY_NAME,
        surfaceWrapper.width,
        surfaceWrapper.height,
        surfaceWrapper.densityDpi,
        surfaceWrapper.surface,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
      )
    width = surfaceWrapper.width
    height = surfaceWrapper.height
    densityDpi = surfaceWrapper.densityDpi
    presentation = Presentation(PresentationContext(context), virtualDisplay?.display)
    presentation?.show()
  }

  protected fun finalize() {
    virtualDisplay?.release()
    virtualDisplay = null
    width = 0
    height = 0
    densityDpi = 0
  }

  companion object {
    const val VIRTUAL_DISPLAY_NAME = "ScreenRendererVirtualDisplay"
  }
}

private fun View.isAttachedTo(presentation: Presentation?): Boolean =
  parent != null && parent == presentation?.findViewById(android.R.id.content)
