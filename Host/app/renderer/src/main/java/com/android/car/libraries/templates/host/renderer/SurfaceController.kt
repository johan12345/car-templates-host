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

import android.view.View
import androidx.car.app.activity.renderer.surface.SurfaceWrapper
import androidx.car.app.serialization.Bundleable
import com.android.car.libraries.apphost.logging.StatusReporter

/** An interface used for presenters who want to present a surface control host. */
interface SurfaceController : StatusReporter {
  /**
   * Returns a surface package object in form of a [Bundleable]. This surface package must be
   * released calling [releaseSurfacePackage]
   */
  fun obtainSurfacePackage(): Bundleable

  /** Releases a surface package previously obtained with [obtainSurfacePackage] */
  fun releaseSurfacePackage(value: Bundleable)

  /** Relayout the surface using the given [width] and [height]. */
  fun relayout(surfaceWrapper: SurfaceWrapper)

  /** Updates the top level content view with given [view]. */
  fun setView(view: View, width: Int, height: Int)

  /** Releases the surface. Should be called once the surface is destroyed. */
  fun releaseSurface()
}
