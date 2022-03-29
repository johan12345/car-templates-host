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

import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;

/** Gesture manager that handles gestures in map-based template presenters. */
public class MapGestureManager {
  /** The minimum span value for the scale event. */
  private static final int MIN_SCALE_SPAN_DP = 10;

  private final ScaleGestureDetector mScaleGestureDetector;
  private final GestureDetector mGestureDetector;
  private final MapOnGestureListener mGestureListener;

  public MapGestureManager(TemplateContext templateContext, long touchUpdateThresholdMillis) {
    Handler touchHandler = new Handler(Looper.getMainLooper());
    mGestureListener = new MapOnGestureListener(templateContext, touchUpdateThresholdMillis);
    mScaleGestureDetector =
        new ScaleGestureDetector(
            templateContext, mGestureListener, touchHandler, MIN_SCALE_SPAN_DP);
    mGestureDetector = new GestureDetector(templateContext, mGestureListener, touchHandler);
  }

  /** Handles the gesture from the given motion event. */
  public void handleGesture(MotionEvent event) {
    mScaleGestureDetector.onTouchEvent(event);
    if (!mScaleGestureDetector.isInProgress()) {
      mGestureDetector.onTouchEvent(event);
    }
  }
}
