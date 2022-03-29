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

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.SurfaceControlViewHost
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.car.app.activity.renderer.surface.SurfaceWrapper
import androidx.car.app.serialization.Bundleable
import com.android.car.libraries.apphost.logging.StatusReporter
import java.io.PrintWriter
import java.lang.IllegalStateException

/** A simple wrapper around [SurfaceControlViewHost] that conforms to [SurfaceController]. */
@RequiresApi(Build.VERSION_CODES.R)
class SurfaceControlViewHostController(val context: Context, val surfaceWrapper: SurfaceWrapper) :
  SurfaceController {

  private var surfaceControlViewHost: SurfaceControlViewHost
  private var width: Int? = null
  private var height: Int? = null

  init {
    val displayManager = context.getSystemService(DisplayManager::class.java)
    val display = displayManager.getDisplay(surfaceWrapper.displayId)
    val hostToken = surfaceWrapper.hostToken
    surfaceControlViewHost = SurfaceControlViewHost(context, display, hostToken)
  }

  /**
   * Because we are wrapping the [SurfacePackage] inside a [Bundleable], automatic releasing is not
   * happening. Instead it must be released manually using [releaseSurfacePackage] once this value
   * has been sent to the remote process.
   *
   * @see [SurfacePackage] Javadoc on recommendations around releasing this value.
   */
  override fun obtainSurfacePackage(): Bundleable {
    val surfacePackage =
      surfaceControlViewHost.surfacePackage
        ?: throw IllegalStateException(
          "SurfaceControlViewHost returned a null " + "SurfacePackage, which should never happen"
        )
    return Bundleable.create(surfacePackage)
  }

  override fun releaseSurfacePackage(value: Bundleable) {
    (value.get() as SurfaceControlViewHost.SurfacePackage).release()
  }

  override fun relayout(surfaceWrapper: SurfaceWrapper) {
    width = surfaceWrapper.width
    height = surfaceWrapper.height
    surfaceControlViewHost.relayout(surfaceWrapper.width, surfaceWrapper.height)
  }

  override fun setView(view: View, width: Int, height: Int) {
    this.width = width
    this.height = height

    // SurfaceControlViewHost doesn't provide a way to detach the view hierarchy once attached.
    // We add an intermediate ViewGroup here so we can detach TemplateView and reuse it in a
    // different surface if needed.
    val contentView = FrameLayout(context)
    contentView.layoutParams =
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    (view.parent as ViewGroup?)?.removeView(view)
    contentView.addView(
      view,
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    )

    surfaceControlViewHost.setView(contentView, width, height)
  }

  override fun releaseSurface() {
    // No-op. Releasing surface is handled by the surface package.
  }

  override fun reportStatus(pw: PrintWriter, piiHandling: StatusReporter.Pii) {
    pw.printf("- display id: %d, width: %d, height: %d\n", surfaceWrapper.displayId, width, height)
  }
}
