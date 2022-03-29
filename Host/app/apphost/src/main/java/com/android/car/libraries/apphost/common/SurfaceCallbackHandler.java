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

/** Interface for handling surface callbacks such as pan and zoom. */
public interface SurfaceCallbackHandler {

  /** Returns whether a new gesture can begin. */
  default boolean canStartNewGesture() {
    return true;
  }

  /**
   * Forwards a scroll gesture event to the car app's {@link
   * androidx.car.app.ISurfaceCallback#onScroll(float, float)}.
   */
  void onScroll(float distanceX, float distanceY);

  /**
   * Forwards a fling gesture event to the car app's {@link
   * androidx.car.app.ISurfaceCallback#onFling(float, float)}.
   */
  void onFling(float velocityX, float velocityY);

  /**
   * Forwards a scale gesture event to the car app's {@link
   * androidx.car.app.ISurfaceCallback#onScale(float, float, float)}.
   */
  void onScale(float focusX, float focusY, float scaleFactor);
}
