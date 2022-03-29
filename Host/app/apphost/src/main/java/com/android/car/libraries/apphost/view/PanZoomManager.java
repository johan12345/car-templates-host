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

import static com.android.car.libraries.apphost.template.view.model.ActionStripWrapper.INVALID_FOCUSED_ACTION_INDEX;

import android.annotation.SuppressLint;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnTouchListener;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import com.android.car.libraries.apphost.common.MapGestureManager;
import com.android.car.libraries.apphost.common.SurfaceCallbackHandler;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.TelemetryEvent;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.template.view.model.ActionStripWrapper;
import com.android.car.libraries.apphost.template.view.model.ActionWrapper;
import java.util.ArrayList;
import java.util.List;

/** A class that manages responses to the user's pan and zoom actions. */
public class PanZoomManager implements OnGenericMotionListener, OnTouchListener {
  /** A delegate class that responds to {@link PanZoomManager}'s actions and queries. */
  public interface Delegate {
    /** Called when the pan mode state changes. */
    void onPanModeChanged(boolean isInPanMode);
  }

  private final TemplateContext mTemplateContext;

  /** A delegate that responds to {@link PanZoomManager}'s actions and queries. */
  private final Delegate mDelegate;

  /** Gesture manager that handles gestures in map-based template presenters. */
  private final MapGestureManager mMapGestureManager;

  /** The amount in pixels to pan with a rotary nudge. */
  private final float mRotaryNudgePanPixels;

  /**
   * Indicates the car app is in the pan mode.
   *
   * <p>In the pan mode, the pan UI and the map action strip are displayed, and other components
   * such as the routing card and action strip are hidden.
   */
  private boolean mIsInPanMode;

  /** Indicates whether the pan manager is enabled or not. */
  private boolean mIsEnabled;

  /** Construct a new instance of {@link PanZoomManager}. */
  public PanZoomManager(
      TemplateContext templateContext,
      Delegate delegate,
      long touchUpdateThresholdMillis,
      float rotaryNudgePanPixels) {
    mTemplateContext = templateContext;
    mDelegate = delegate;
    mMapGestureManager = new MapGestureManager(templateContext, touchUpdateThresholdMillis);
    mRotaryNudgePanPixels = rotaryNudgePanPixels;
  }

  @Override
  public boolean onGenericMotion(View v, MotionEvent event) {
    // If we are not in the pan mode or the pan manager is disabled, do not intercept the motion
    // events. Also, do not intercept rotary controller scrolls.
    if (!mIsInPanMode || !mIsEnabled || event.getAction() == MotionEvent.ACTION_SCROLL) {
      return false;
    }

    handleGesture(event);
    return true;
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouch(View v, MotionEvent event) {
    // Handle gestures only when the pan manager is enabled.
    if (!mIsEnabled) {
      return false;
    }

    handleGesture(event);
    return true;
  }

  /** Handles the gesture from the given motion event. */
  public void handleGesture(MotionEvent event) {
    mMapGestureManager.handleGesture(event);
  }

  /**
   * Handles the rotary inputs by translating them to pan events, if appropriate.
   *
   * @return {@code true} if the input was handled, {@code false} otherwise.
   */
  public boolean handlePanEventsIfNeeded(int keyCode) {
    // When in the pan mode, use the rotary nudges for map panning.
    if (mIsInPanMode) {
      SurfaceCallbackHandler handler = mTemplateContext.getSurfaceCallbackHandler();
      if (!handler.canStartNewGesture()) {
        return false;
      }

      float distanceX = 0f;
      float distanceY = 0f;
      if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
        distanceX = -mRotaryNudgePanPixels;
      } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
        distanceX = mRotaryNudgePanPixels;
      } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
        distanceY = -mRotaryNudgePanPixels;
      } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
        distanceY = mRotaryNudgePanPixels;
      }

      if (distanceX != 0 || distanceY != 0) {
        // each rotary nudge is treated as a single gesture.
        handler.onScroll(distanceX, distanceY);
        mTemplateContext
            .getTelemetryHandler()
            .logCarAppTelemetry(TelemetryEvent.newBuilder(UiAction.ROTARY_PAN));

        return true;
      }
    }

    return false;
  }

  /**
   * Enables or disables the pan manager.
   *
   * <p>If the pan mode was active when the pan manager is disabled, it will become inactive.
   */
  public void setEnabled(boolean isEnabled) {
    mIsEnabled = isEnabled;

    if (mIsInPanMode && !isEnabled) {
      // If the user is in the pan mode but the feature is disabled, exit pan mode.
      setPanMode(false);
    }
  }

  /**
   * Returns the map {@link ActionStripWrapper} from the given {@link ActionStrip}.
   *
   * <p>This method contains the special handling logic for {@link Action#PAN} buttons.
   */
  public ActionStripWrapper getMapActionStripWrapper(
      TemplateContext templateContext, ActionStrip actionStrip) {
    List<ActionWrapper> mapActions = new ArrayList<>();
    int focusedActionIndex = INVALID_FOCUSED_ACTION_INDEX;

    int actionIndex = 0;
    for (Action action : actionStrip.getActions()) {
      ActionWrapper.Builder builder = new ActionWrapper.Builder(action);
      if (action.getType() == Action.TYPE_PAN) {
        if (templateContext.getInputConfig().hasTouch()) {
          // Hide the pan button in touch screens.
          continue;
        } else {
          // React to the pan button in the rotary and touchpad mode.
          builder.setOnClickListener(() -> setPanMode(!mIsInPanMode));

          // Keep the focus on the pan button if the user uses a touchpad and is in the pan mode,
          // because the user cannot move the focus with the touchpad in the pan mode.
          if (mIsInPanMode && templateContext.getInputConfig().hasTouchpadForUiNavigation()) {
            focusedActionIndex = actionIndex;
          }
        }
      }
      mapActions.add(builder.build());
      actionIndex++;
    }

    return new ActionStripWrapper.Builder(mapActions)
        .setFocusedActionIndex(focusedActionIndex)
        .build();
  }

  /** Returns whether the pan mode is active or not. */
  public boolean isInPanMode() {
    return mIsInPanMode;
  }

  /**
   * Sets the pan mode.
   *
   * <p>When the pan mode changes, the delegate will be notified of the change.
   */
  @VisibleForTesting
  void setPanMode(boolean isInPanMode) {
    boolean panModeChanged = mIsInPanMode != isInPanMode;
    mIsInPanMode = isInPanMode;

    if (panModeChanged) {
      mDelegate.onPanModeChanged(isInPanMode);
    }
  }
}
