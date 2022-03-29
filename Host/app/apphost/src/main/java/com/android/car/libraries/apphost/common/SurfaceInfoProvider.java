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

package com.android.car.libraries.apphost.common;

import android.graphics.Rect;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A class for storing and retrieving the properties such as the visible area and stable center of a
 * surface.
 */
public interface SurfaceInfoProvider {
  /**
   * Returns the {@link Rect} that specifies the region in the view where the templated-content
   * (e.g. the card container, FAB) currently extends to. Returns {@code null} if the value is not
   * set.
   */
  @Nullable Rect getVisibleArea();

  /**
   * Sets the safe area and if needed updates the stable center.
   *
   * <p>Subscribe to the event {@link EventManager.EventType#SURFACE_VISIBLE_AREA} to be notify when
   * the safe area has been updated.
   */
  void setVisibleArea(Rect safeArea);

  /**
   * Returns the {@link Rect} that specifies the region of the stable visible area where the
   * templated content (e.g. card container, action strip) could possibly extend to. It is stable in
   * that the area is the guaranteed visible no matter any dynamic changes to the view. It is
   * possible for stable area to increase or decrease due to changes in the template content or a
   * template change.
   */
  @Nullable Rect getStableArea();

  /** Indicates that the stable area should be recalculated the next time the safe area is set. */
  void invalidateStableArea();
}
