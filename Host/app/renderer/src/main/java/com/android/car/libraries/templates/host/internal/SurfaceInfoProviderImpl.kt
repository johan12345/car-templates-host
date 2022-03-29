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

import android.graphics.Rect
import com.android.car.libraries.apphost.common.EventManager
import com.android.car.libraries.apphost.common.EventManager.EventType
import com.android.car.libraries.apphost.common.SurfaceInfoProvider

/** Provides surface properties necessary for efficiently rendering partial content. */
// TODO (b/206636788): Consolidate redundant code and improve documentation
internal class SurfaceInfoProviderImpl(private val eventManager: EventManager) :
  SurfaceInfoProvider {
  private var visibleArea: Rect? = null
  private var stableArea: Rect? = null

  override fun getVisibleArea() = visibleArea
  override fun getStableArea() = stableArea

  override fun setVisibleArea(area: Rect) {
    val currentAreaNeedUpdated = visibleArea == null || area != visibleArea
    visibleArea = area
    if (currentAreaNeedUpdated) {
      eventManager.dispatchEvent(EventType.SURFACE_VISIBLE_AREA)
    }

    val stableAreaToUpdate = calculateStableArea(area, stableArea)
    if (stableArea != stableAreaToUpdate) {
      stableArea = stableAreaToUpdate
      eventManager.dispatchEvent(EventType.SURFACE_STABLE_AREA)
    }
  }

  private fun calculateStableArea(visibleArea: Rect, stableArea: Rect?): Rect {
    return if (stableArea == null || !stableArea.setIntersect(stableArea, visibleArea)) {
      visibleArea
    } else {
      stableArea
    }
  }

  override fun invalidateStableArea() {
    stableArea = null
  }
}
