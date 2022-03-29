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


import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import com.android.car.libraries.apphost.common.StatusBarManager.StatusBarState;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;

/**
 * Abstract base class for {@link TemplatePresenter}s which have a {@link
 * androidx.car.app.SurfaceContainer}.
 */
public abstract class AbstractSurfaceTemplatePresenter extends AbstractTemplatePresenter
    implements PanZoomManager.Delegate {
  /** The time threshold between touch events for 30fps updates. */
  private static final long TOUCH_UPDATE_THRESHOLD_MILLIS = 30;

  /** The amount in pixels to pan with a rotary nudge. */
  private static final float ROTARY_NUDGE_PAN_PIXELS = 50f;

  private final OnGlobalLayoutListener mGlobalLayoutListener =
      new OnGlobalLayoutListener() {
        @SuppressWarnings("nullness") // suppress under initialization warning for this
        @Override
        public void onGlobalLayout() {
          if (mShouldUpdateVisibleArea) {
            AbstractSurfaceTemplatePresenter.this.updateVisibleArea();
            mShouldUpdateVisibleArea = false;
          }
        }
      };

  /** Gesture manager that handles pan and zoom gestures in map-based template presenters. */
  private final PanZoomManager mPanZoomManager;

  /**
   * A boolean flag that indicates whether the visible area should be updated in the next layout
   * phase.
   */
  private boolean mShouldUpdateVisibleArea;

  /**
   * Constructs a new instance of a {@link AbstractTemplatePresenter} with the given {@link
   * Template}.
   */
  @SuppressWarnings({"nullness:method.invocation", "nullness:assignment", "nullness:argument"})
  public AbstractSurfaceTemplatePresenter(
      TemplateContext templateContext,
      TemplateWrapper templateWrapper,
      StatusBarState statusBarState) {
    super(templateContext, templateWrapper, statusBarState);

    mPanZoomManager =
        new PanZoomManager(
            templateContext, this, getTouchUpdateThresholdMillis(), getRotaryNudgePanPixels());
  }

  @Override
  public void onPause() {
    getView().getViewTreeObserver().removeOnGlobalLayoutListener(mGlobalLayoutListener);
    getView().setOnTouchListener(null);
    getView().setOnGenericMotionListener(null);
    super.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
    getView().getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);

    L.d(
        LogTags.TEMPLATE,
        "Pan and zoom is %s in %s",
        isPanAndZoomEnabled() ? "ENABLED" : "DISABLED",
        getTemplate());
    if (isPanAndZoomEnabled()) {
      getView().setOnTouchListener(mPanZoomManager);
      getView().setOnGenericMotionListener(mPanZoomManager);
    }
  }

  @Override
  public boolean usesSurface() {
    return true;
  }

  @Override
  public boolean isFullScreen() {
    return false;
  }

  /** Adjusts the {@code inset} according to the views visible on screen. */
  public abstract void calculateAdditionalInset(Rect inset);

  @Override
  public void onPanModeChanged(boolean isInPanMode) {
    // No-op by default
  }

  /** Returns whether the pan and zoom feature is enabled. */
  public boolean isPanAndZoomEnabled() {
    return false;
  }

  /** Returns the time threshold in milliseconds for processing touch events. */
  public long getTouchUpdateThresholdMillis() {
    return TOUCH_UPDATE_THRESHOLD_MILLIS;
  }

  /** Returns the amount in pixels to pan with a rotary nudge. */
  public float getRotaryNudgePanPixels() {
    return ROTARY_NUDGE_PAN_PIXELS;
  }

  /** Returns the {@link OnGlobalLayoutListener} instance attached to the view tree. */
  @VisibleForTesting
  public OnGlobalLayoutListener getGlobalLayoutListener() {
    return mGlobalLayoutListener;
  }

  /** Returns the {@link PanZoomManager} instance associated with this presenter. */
  protected PanZoomManager getPanZoomManager() {
    return mPanZoomManager;
  }

  /** Requests an update to the surface's visible area information. */
  protected void requestVisibleAreaUpdate() {
    // Flip the flag so that we will update the visible area in our next layout phase. We cannot
    // just update the visible area here because the views are not laid out when they are just
    // inflated, which means that we cannot use the view coordinates to calculate where the views
    // are not drawn.
    mShouldUpdateVisibleArea = true;
  }

  private void updateVisibleArea() {
    View rootView = getView();
    Rect safeAreaInset = new Rect();
    safeAreaInset.left = rootView.getLeft() + rootView.getPaddingLeft();
    safeAreaInset.top = rootView.getTop() + rootView.getPaddingTop();
    safeAreaInset.bottom = rootView.getBottom() - rootView.getPaddingBottom();
    safeAreaInset.right = rootView.getRight() - rootView.getPaddingRight();
    calculateAdditionalInset(safeAreaInset);
    getTemplateContext().getSurfaceInfoProvider().setVisibleArea(safeAreaInset);
  }
}
