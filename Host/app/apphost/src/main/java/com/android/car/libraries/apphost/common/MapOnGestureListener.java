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
import android.os.SystemClock;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.InputDevice;
import android.view.MotionEvent;
import com.android.car.libraries.apphost.common.ScaleGestureDetector.OnScaleGestureListener;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryEvent;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Gesture listener in map-based template presenters.
 *
 * <p>The following events are rate-limited to reduce the delay between touch gestures and the app
 * response:
 *
 * <ul>
 *   <li>{@link #onScroll(MotionEvent, MotionEvent, float, float)}
 *   <li>{@link #onScale(ScaleGestureDetector)}
 * </ul>
 */
public class MapOnGestureListener extends SimpleOnGestureListener
    implements OnScaleGestureListener {
  /** Maximum number of debug overlay texts to display. */
  private static final int MAX_DEBUG_OVERLAY_LINES = 3;

  /** The scale factor to send to the app when the user double taps on the screen. */
  private static final float DOUBLE_TAP_ZOOM_FACTOR = 2f;

  private final DecimalFormat mDecimalFormat = new DecimalFormat("#.##");

  private final Deque<String> mDebugOverlayTexts = new ArrayDeque<>();

  private final TemplateContext mTemplateContext;

  /** The time threshold between touch events. */
  private final long mTouchUpdateThresholdMillis;

  /** The last time that a scroll touch event happened. */
  private long mScrollLastTouchTimeMillis;

  /** The last time that a scale touch event happened. */
  private long mScaleLastTouchTimeMillis;

  /** The scroll distance in the X axis since the last distance update to the car app. */
  private float mCumulativeDistanceX;

  /** The scroll distance in the Y axis since the last distance update to the car app. */
  private float mCumulativeDistanceY;

  /**
   * A flag that indicates that the scale gesture just ended.
   *
   * <p>This flag is used to work around the issue where a fling gesture is detected when a scale
   * event ends.
   */
  private boolean mScaleJustEnded;

  /** A flag that indicates that user is currently scrolling. */
  private boolean mIsScrolling;

  public MapOnGestureListener(TemplateContext templateContext, long touchUpdateThresholdMillis) {
    this.mTemplateContext = templateContext;
    this.mTouchUpdateThresholdMillis = touchUpdateThresholdMillis;
  }

  @Override
  public boolean onDown(MotionEvent e) {
    L.d(LogTags.TEMPLATE, "Down touch event detected");
    // Reset the flag that indicates that a sequence of scroll events may be starting from this
    // point.
    mIsScrolling = false;

    mCumulativeDistanceX = 0;
    mCumulativeDistanceY = 0;
    return super.onDown(e);
  }

  @Override
  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    long touchTimeMillis = SystemClock.uptimeMillis();

    // If this is the first scroll event in a series of gestures, log a telemetry event.
    // This avoids triggering more than one event per sequence from finger touching down to
    // finger lifted off the screen.
    if (!mIsScrolling) {
      // Since this is essentially the beginning of the scroll gesture, we need to check if
      // SurfaceCallbackHandler allows the scroll to begin (e.g. checking against whether the
      // user is already interacting with the screen too often).
      SurfaceCallbackHandler handler = mTemplateContext.getSurfaceCallbackHandler();
      if (!handler.canStartNewGesture()) {
        mCumulativeDistanceX = 0;
        mCumulativeDistanceY = 0;
        return true;
      }

      mIsScrolling = true;
      mTemplateContext
          .getTelemetryHandler()
          .logCarAppTelemetry(TelemetryEvent.newBuilder(UiAction.PAN));
    }

    mCumulativeDistanceX += distanceX;
    mCumulativeDistanceY += distanceY;

    if (touchTimeMillis - mScrollLastTouchTimeMillis > mTouchUpdateThresholdMillis) {
      mTemplateContext
          .getSurfaceCallbackHandler()
          .onScroll(mCumulativeDistanceX, mCumulativeDistanceY);
      mScrollLastTouchTimeMillis = touchTimeMillis;

      // Reset the cumulative distance.
      mCumulativeDistanceX = 0;
      mCumulativeDistanceY = 0;

      if (mTemplateContext.getDebugOverlayHandler().isActive()) {
        updateDebugOverlay(
            "scroll distance [X: "
                + mDecimalFormat.format(distanceX)
                + ", Y: "
                + mDecimalFormat.format(distanceY)
                + "]");
      }
    }

    return true;
  }

  @Override
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    // Do not send fling events when the scale event just ended. This works around the issue
    // where a fling gesture is detected when a scale event ends.
    if (!mScaleJustEnded) {
      // Note that unlike the scroll, scale and double-tap events, onFling happens at the end of
      // scroll events, so we do not check against SurfaceCallbackHandler#canStartNewGesture.
      mTemplateContext.getSurfaceCallbackHandler().onFling(velocityX, velocityY);

      if (mTemplateContext.getDebugOverlayHandler().isActive()) {
        updateDebugOverlay(
            "fling velocity [X: "
                + mDecimalFormat.format(velocityX)
                + ", Y: "
                + mDecimalFormat.format(velocityY)
                + "]");
      }

      mTemplateContext
          .getTelemetryHandler()
          .logCarAppTelemetry(TelemetryEvent.newBuilder(UiAction.FLING));
    } else {
      mScaleJustEnded = false;
    }
    return true;
  }

  @Override
  public boolean onDoubleTap(MotionEvent e) {
    SurfaceCallbackHandler handler = mTemplateContext.getSurfaceCallbackHandler();
    if (!handler.canStartNewGesture()) {
      return false;
    }

    float x = e.getX();
    float y = e.getY();

    // We cannot reliably map the touch pad position to the screen position.
    // If the double tap happened in a touch pad, zoom into the center of the surface.
    if (e.getSource() == InputDevice.SOURCE_TOUCHPAD) {
      Rect visibleArea = mTemplateContext.getSurfaceInfoProvider().getVisibleArea();
      if (visibleArea != null) {
        x = visibleArea.centerX();
        y = visibleArea.centerY();
      } else {
        // If we do not know the visible area, send negative focal point values to indicate
        // that it is unavailable.
        x = -1;
        y = -1;
      }
    }

    handler.onScale(x, y, DOUBLE_TAP_ZOOM_FACTOR);

    if (mTemplateContext.getDebugOverlayHandler().isActive()) {
      updateDebugOverlay(
          "scale focus [X: "
              + mDecimalFormat.format(x)
              + ", Y: "
              + mDecimalFormat.format(y)
              + "], factor ["
              + DOUBLE_TAP_ZOOM_FACTOR
              + "]");
    }

    mTemplateContext
        .getTelemetryHandler()
        .logCarAppTelemetry(TelemetryEvent.newBuilder(UiAction.ZOOM));

    return true;
  }

  @Override
  public boolean onScale(ScaleGestureDetector detector) {
    long touchTimeMillis = SystemClock.uptimeMillis();
    boolean shouldSendScaleEvent =
        touchTimeMillis - mScaleLastTouchTimeMillis > mTouchUpdateThresholdMillis;
    if (shouldSendScaleEvent) {
      handleScale(detector);
      mScaleLastTouchTimeMillis = touchTimeMillis;
    }

    // If we return false here, the detector will continue accumulating the scale factor until
    // the next time we return true.
    return shouldSendScaleEvent;
  }

  @Override
  public boolean onScaleBegin(ScaleGestureDetector detector) {
    // We need to check if SurfaceCallbackHandler allows the scaling gesture to begin (e.g. checking
    // against whether the user is already interacting with the screen too often). Returning false
    // here if needed to tell the detector to ignore the rest of the gesture.
    SurfaceCallbackHandler handler = mTemplateContext.getSurfaceCallbackHandler();
    return handler.canStartNewGesture();
  }

  @Override
  public void onScaleEnd(ScaleGestureDetector detector) {
    handleScale(detector);
    mScaleJustEnded = true;

    mTemplateContext
        .getTelemetryHandler()
        .logCarAppTelemetry(TelemetryEvent.newBuilder(UiAction.ZOOM));
  }

  private void handleScale(ScaleGestureDetector detector) {
    // The focus values are only meaningful when the motion is in progress
    if (detector.isInProgress()) {
      float focusX = detector.getFocusX();
      float focusY = detector.getFocusY();
      float scaleFactor = detector.getScaleFactor();
      mTemplateContext.getSurfaceCallbackHandler().onScale(focusX, focusY, scaleFactor);

      if (mTemplateContext.getDebugOverlayHandler().isActive()) {
        updateDebugOverlay(
            "scale focus [X: "
                + mDecimalFormat.format(focusX)
                + ", Y: "
                + mDecimalFormat.format(focusY)
                + "], factor ["
                + mDecimalFormat.format(scaleFactor)
                + "]");
      }
    }
  }

  private void updateDebugOverlay(String debugText) {
    if (mDebugOverlayTexts.size() >= MAX_DEBUG_OVERLAY_LINES) {
      mDebugOverlayTexts.removeFirst();
    }
    mDebugOverlayTexts.addLast(debugText);

    StringBuilder sb = new StringBuilder();
    for (String text : mDebugOverlayTexts) {
      sb.append(text);
      sb.append("\n");
    }

    // Remove the last newline.
    sb.setLength(sb.length() - 1);
    mTemplateContext.getDebugOverlayHandler().updateDebugOverlayEntry("Gesture", sb.toString());
  }
}
