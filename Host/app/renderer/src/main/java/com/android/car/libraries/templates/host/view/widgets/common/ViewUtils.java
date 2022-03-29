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

package com.android.car.libraries.templates.host.view.widgets.common;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import androidx.annotation.Nullable;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryEvent;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.logging.TelemetryHandler;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/** Utility class for view operations. */
public class ViewUtils {
  private ViewUtils() {}

  /** A {@link TouchDelegate} that allows combining multiple {@link TouchDelegate}s into one */
  private static class TouchDelegateComposite extends TouchDelegate {
    private static class TouchDelegateInfo {
      final TouchDelegate mTouchDelegate;
      @Nullable final WeakReference<View> mTargetView;

      TouchDelegateInfo(TouchDelegate touchDelegate, @Nullable View targetView) {
        mTouchDelegate = touchDelegate;
        mTargetView = targetView != null ? new WeakReference<>(targetView) : null;
      }
    }

    private final List<TouchDelegateInfo> delegates = new ArrayList<>();

    private static final Rect emptyRect = new Rect();

    public TouchDelegateComposite(View view) {
      super(emptyRect, view);
    }

    public void addDelegate(TouchDelegate delegate, @Nullable View targetView) {
      if (delegate != null) {
        delegates.add(new TouchDelegateInfo(delegate, targetView));
      }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      boolean res = false;
      float x = event.getX();
      float y = event.getY();
      for (TouchDelegateInfo delegateInfo : delegates) {
        event.setLocation(x, y);
        if (delegateInfo.mTargetView != null && delegateInfo.mTargetView.get() == null) {
          throw new IllegalStateException("Invalid touch delegation, target view has be removed");
        }
        res = delegateInfo.mTouchDelegate.onTouchEvent(event) || res;
      }
      return res;
    }
  }

  // Returns true if {@code child} is a descendant of {@code parent}.
  private static boolean isDescendant(View parent, View child) {
    View current = child;
    while (current != null) {
      if (current == parent) {
        return true;
      }
      if (!(current.getParent() instanceof View)) {
        return false;
      }
      current = (View) current.getParent();
    }
    return false;
  }

  /**
   * Sets the tap target for the given view to encompass at least the area of a square of the given
   * dimensions.
   *
   * <p>If the current tap area is already larger in either dimension, method will not shrink it
   * (hence "min" tap target).
   *
   * <p>If the current tap area is smaller, method will expand it equally on either side to meet the
   * minimum size.
   *
   * <p><b>Important: This method works by adding a {@link TouchDelegate} to the container view.</b>
   * The caller must make sure this method is invoked only once per view. Otherwise, multiple {@link
   * TouchDelegate} instances will be added to the container, which could cause duplicate click
   * events.
   *
   * @param containerView the view where a {@link TouchDelegate} will be added.
   * @param view the view to potentially expand the tap target for.
   * @param tapTargetSize the dimensions of a square that will become the new minimum tap target for
   *     the given view.
   */
  public static void setMinTapTarget(ViewGroup containerView, View view, int tapTargetSize) {
    containerView.post(
        () -> {
          // Return if the view has already been removed from the view hierarchy or has unexpected
          // parent.
          if (!(view.getParent() instanceof View)
              || !isDescendant(containerView, (View) view.getParent())) {
            L.d(LogTags.TEMPLATE, "Cannot set min tap target for view %s", view);
            return;
          }

          Rect rect = new Rect();
          view.getHitRect(rect);
          containerView.offsetDescendantRectToMyCoords((View) view.getParent(), rect);

          int rectHeight = rect.height();
          if (rectHeight < tapTargetSize) {
            int delta = (tapTargetSize - rectHeight) / 2;
            rect.top -= delta;
            rect.bottom += delta;
          }

          int rectWidth = rect.width();
          if (rectWidth < tapTargetSize) {
            int delta = (tapTargetSize - rectWidth) / 2;
            rect.left -= delta;
            rect.right += delta;
          }

          TouchDelegate parentTouchDelegate = containerView.getTouchDelegate();
          TouchDelegate newDelegate = new TouchDelegate(rect, view);
          if (parentTouchDelegate != null) {
            if (parentTouchDelegate instanceof TouchDelegateComposite) {
              ((TouchDelegateComposite) parentTouchDelegate).addDelegate(newDelegate, view);
              newDelegate = parentTouchDelegate;
            } else {
              TouchDelegateComposite composite = new TouchDelegateComposite(view);
              composite.addDelegate(parentTouchDelegate, null);
              composite.addDelegate(newDelegate, view);
              newDelegate = composite;
            }
          }
          containerView.setTouchDelegate(newDelegate);
        });
  }

  /**
   * Enforce the minimum and maximum size limit to the given view.
   *
   * <p>The view width and height sizes must be equal.
   */
  public static void enforceViewSizeLimit(View view, int minSize, int maxSize) {
    enforceViewSizeLimit(
        view,
        /* minWidth= */ minSize,
        /* maxWidth= */ maxSize,
        /* minHeight= */ minSize,
        /* maxHeight= */ maxSize);
  }

  /** Enforce the minimum and maximum width and height limits to the given view. */
  public static void enforceViewSizeLimit(
      View view, int minWidth, int maxWidth, int minHeight, int maxHeight) {
    LayoutParams layoutParams = view.getLayoutParams();
    if (layoutParams == null) {
      return;
    }

    int width = getValueInRange(layoutParams.width, minWidth, maxWidth);
    int height = getValueInRange(layoutParams.height, minHeight, maxHeight);
    layoutParams.width = width;
    layoutParams.height = height;
    view.setLayoutParams(layoutParams);
  }

  /** Logs a telemetry event with the given {@link UiAction} and {@link TemplateContext} */
  public static void logCarAppTelemetry(TemplateContext templateContext, UiAction action) {
    logCarAppTelemetry(
        templateContext,
        TelemetryEvent.newBuilder(action)
            .setComponentName(templateContext.getCarAppPackageInfo().getComponentName()));
  }

  /**
   * Logs a telemetry event with the given {@link UiAction}, action count and {@link
   * TemplateContext}
   */
  public static void logCarAppTelemetry(
      TemplateContext templateContext, UiAction action, int actionCount) {
    logCarAppTelemetry(
        templateContext,
        TelemetryEvent.newBuilder(action)
            .setComponentName(templateContext.getCarAppPackageInfo().getComponentName())
            .setItemsLoadedCount(actionCount));
  }

  /**
   * Logs a telemetry event with the given {@link TelemetryEvent.Builder} and {@link
   * TemplateContext}
   */
  public static void logCarAppTelemetry(
      TemplateContext templateContext, TelemetryEvent.Builder builder) {
    TelemetryHandler telemetry = templateContext.getTelemetryHandler();
    telemetry.logCarAppTelemetry(builder);
  }

  /**
   * Returns the capped value between the min and max range.
   *
   * <p>If the given value is less than or equal to 0 (e.g. MATCH_CONSTRAINT (0), MATCH_PARENT (-1),
   * or WRAP_CONTENT (-2)), the original value will be returned.
   */
  private static int getValueInRange(int value, int min, int max) {
    if (value <= 0) {
      return value;
    }

    int newValue = value;
    newValue = min(newValue, max);
    newValue = max(newValue, min);
    return newValue;
  }
}
