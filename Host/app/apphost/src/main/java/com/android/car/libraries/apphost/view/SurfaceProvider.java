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
package com.android.car.libraries.apphost.view;

import android.view.Surface;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A provider for {@link Surface}s that can be used by 3p apps to render custom content. */
public interface SurfaceProvider {
  /** Listener interface for the {@link SurfaceProvider}. */
  interface SurfaceProviderListener {
    /**
     * Notifies the listener that the surface was created.
     *
     * <p>Clients should use this callback to prepare for drawing.
     */
    void onSurfaceCreated();

    /**
     * Notifies the listener that the surface had some structural changes (format or size).
     *
     * <p>This is called at least once after {@link #onSurfaceCreated()}. Clients must update the
     * imagery on the surface.
     */
    void onSurfaceChanged();

    /**
     * Notifies the listener that the surface is being destroyed.
     *
     * <p>After returning from this call clients should not try to access the surface anymore. The
     * {@link SurfaceProvider} is still valid after this call and may be followed by a {@link
     * #onSurfaceCreated()}.
     */
    void onSurfaceDestroyed();

    /** Notifies the listener about a surface scroll touch event. */
    void onSurfaceScroll(float distanceX, float distanceY);

    /** Notifies the listener about a surface fling touch event. */
    void onSurfaceFling(float velocityX, float velocityY);

    /** Notifies the listener about a surface scale touch event. */
    void onSurfaceScale(float focusX, float focusY, float scaleFactor);
  }

  /**
   * Sets the listener which is called when {@link Surface} changes such as on creation, destruction
   * or due to structural changes.
   */
  void setListener(@Nullable SurfaceProviderListener listener);

  /** Returns the {@link Surface} that this provider manages. */
  @Nullable Surface getSurface();

  /** Returns the width of the surface,m in pixels. */
  int getWidth();

  /** Returns the height of the surface, in pixels. */
  int getHeight();

  /** The screen density expressed as dots-per-inch. */
  int getDpi();

  SurfaceProvider EMPTY =
      new SurfaceProvider() {
        @Override
        public void setListener(@Nullable SurfaceProviderListener listener) {}

        @Override
        @Nullable
        public Surface getSurface() {
          return null;
        }

        @Override
        public int getWidth() {
          return 0;
        }

        @Override
        public int getHeight() {
          return 0;
        }

        @Override
        public int getDpi() {
          return 0;
        }
      };
}
