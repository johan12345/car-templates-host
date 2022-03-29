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

import static android.view.View.VISIBLE;
import static java.lang.Math.max;

import android.graphics.Insets;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener;
import android.view.ViewTreeObserver.OnTouchModeChangeListener;
import android.view.WindowInsets;
import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleRegistry;
import com.android.car.libraries.apphost.common.StatusBarManager.StatusBarState;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryEvent;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.logging.TelemetryHandler;
import java.util.List;

/**
 * Abstract base class for {@link TemplatePresenter}s which implements some of the common presenter
 * functionality.
 */
public abstract class AbstractTemplatePresenter implements TemplatePresenter {
  /**
   * Test-only override for {@link #hasWindowFocus()}, since robolectric does not set the window
   * focus properly.
   */
  @VisibleForTesting public Boolean mHasWindowFocusOverride;

  private final TemplateContext mTemplateContext;
  private final LifecycleRegistry mLifecycleRegistry;
  private final StatusBarState mStatusBarState;

  private TemplateWrapper mTemplateWrapper;

  /** The last focused view before the presenter was refreshed. */
  @Nullable private View mLastFocusedView;

  /**
   * Returns a callback called when the presenter view's touch mode changes.
   *
   * @see #restoreFocus() for details on how we work around a focus-related GMS core bug
   */
  private final OnTouchModeChangeListener mOnTouchModeChangeListener =
      new OnTouchModeChangeListener() {
        @SuppressWarnings("nullness") // suppress under initialization warning for this
        @Override
        public void onTouchModeChanged(boolean isInTouchMode) {
          if (!isInTouchMode) {
            restoreFocus();
          }
        }
      };

  /**
   * Returns a callback called when the presenter view's focus changes.
   *
   * @see #restoreFocus() for details on how we work around a focus-related GMS core bug
   */
  private final OnGlobalFocusChangeListener mOnGlobalFocusChangeListener =
      new OnGlobalFocusChangeListener() {
        @SuppressWarnings("nullness") // suppress under initialization warning for this
        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
          if (newFocus != null) {
            setLastFocusedView(newFocus);
          }
        }
      };

  /**
   * Constructs a new instance of a {@link AbstractTemplatePresenter} with the given {@link
   * Template}.
   */
  // Suppress under-initialization checker warning for passing this to the LifecycleRegistry's
  // ctor.
  @SuppressWarnings({"nullness:assignment", "nullness:argument"})
  public AbstractTemplatePresenter(
      TemplateContext templateContext,
      TemplateWrapper templateWrapper,
      StatusBarState statusBarState) {
    mTemplateContext = templateContext;
    mTemplateWrapper = TemplateWrapper.copyOf(templateWrapper);
    mStatusBarState = statusBarState;
    mLifecycleRegistry = new LifecycleRegistry(this);
  }

  /** Sets the template this presenter will produce the views for. */
  @Override
  public void setTemplate(TemplateWrapper templateWrapper) {
    mTemplateWrapper = TemplateWrapper.copyOf(templateWrapper);

    onTemplateChanged();

    if (!templateWrapper.isRefresh()) {
      // Some presenters may get reused even if the template is not a refresh of the previous one.
      // In those instances, we want the focus to be set to where the default focus should be
      // instead of last focussed element. Specifically, we want to clear existing focus first,
      // because if the previous focus was on a row item, and the list is reused and scrolled
      // to the top, calling setDefaultFocus itself would not reset the focus back to the first
      // row item.
      getView().clearFocus();
      setDefaultFocus();
    } else {
      View focusedView = getView().findFocus();
      if (focusedView != null && focusedView.getVisibility() == VISIBLE) {
        setLastFocusedView(focusedView);
      } else {
        setDefaultFocus();
      }
    }
  }

  /** Returns the template associated with this presenter. */
  @Override
  public Template getTemplate() {
    return mTemplateWrapper.getTemplate();
  }

  /**
   * Returns the {@link TemplateWrapper} instance that wraps the template associated with this
   * presenter.
   *
   * @see #getTemplate()
   */
  @Override
  public TemplateWrapper getTemplateWrapper() {
    return mTemplateWrapper;
  }

  /** Returns the {@link TemplateContext} instance associated with this presenter. */
  @Override
  public TemplateContext getTemplateContext() {
    return mTemplateContext;
  }

  @Override
  @CallSuper
  public void onCreate() {
    L.d(LogTags.TEMPLATE, "Presenter onCreate: %s", this);
    mLifecycleRegistry.setCurrentState(State.CREATED);
  }

  @Override
  @CallSuper
  public void onDestroy() {
    L.d(LogTags.TEMPLATE, "Presenter onDestroy: %s", this);
    mLifecycleRegistry.setCurrentState(State.DESTROYED);
  }

  @Override
  @CallSuper
  public void onStart() {
    L.d(LogTags.TEMPLATE, "Presenter onStart: %s", this);
    mLifecycleRegistry.setCurrentState(State.STARTED);
  }

  @Override
  @CallSuper
  public void onStop() {
    L.d(LogTags.TEMPLATE, "Presenter onStop: %s", this);
    mLifecycleRegistry.setCurrentState(State.CREATED);
  }

  @Override
  @CallSuper
  public void onResume() {
    L.d(LogTags.TEMPLATE, "Presenter onResume: %s", this);
    mLifecycleRegistry.setCurrentState(State.RESUMED);
    mTemplateContext.getStatusBarManager().setStatusBarState(mStatusBarState, getView());

    ViewTreeObserver viewTreeObserver = getView().getViewTreeObserver();
    viewTreeObserver.addOnTouchModeChangeListener(mOnTouchModeChangeListener);
    viewTreeObserver.addOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);
  }

  @Override
  @CallSuper
  public void onPause() {
    L.d(LogTags.TEMPLATE, "Presenter onPause: %s", this);
    mLifecycleRegistry.setCurrentState(State.STARTED);

    ViewTreeObserver viewTreeObserver = getView().getViewTreeObserver();
    viewTreeObserver.removeOnTouchModeChangeListener(mOnTouchModeChangeListener);
    viewTreeObserver.removeOnGlobalFocusChangeListener(mOnGlobalFocusChangeListener);

    if (mTemplateContext.getColorContrastCheckState().checksContrast()) {
      sendColorContrastTelemetryEvent(mTemplateContext, getTemplate().getClass().getSimpleName());
    }
  }

  @Override
  public void applyWindowInsets(WindowInsets windowInsets, int minimumTopPadding) {
    int leftInset;
    int topInset;
    int rightInset;
    int bottomInset;
    if (VERSION.SDK_INT >= VERSION_CODES.R) {
      Insets insets =
          windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
      leftInset = insets.left;
      topInset = insets.top;
      rightInset = insets.right;
      bottomInset = insets.bottom;

    } else {
      leftInset = windowInsets.getSystemWindowInsetLeft();
      topInset = windowInsets.getSystemWindowInsetTop();
      rightInset = windowInsets.getSystemWindowInsetRight();
      bottomInset = windowInsets.getSystemWindowInsetBottom();
    }

    View v = getView();
    v.setPadding(leftInset, max(topInset, minimumTopPadding), rightInset, bottomInset);
  }

  @Override
  public boolean setDefaultFocus() {
    View defaultFocusedView = getDefaultFocusedView();
    if (defaultFocusedView != null) {
      defaultFocusedView.requestFocus();
      setLastFocusedView(defaultFocusedView);
    }
    return true;
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
    return false;
  }

  @Override
  public boolean onPreDraw() {
    return true;
  }

  @Override
  public String toString() {
    return "["
        + Integer.toHexString(hashCode())
        + ": "
        + mTemplateWrapper.getTemplate().getClass().getSimpleName()
        + "]";
  }

  /** Indicates that the template set in the presenter has changed. */
  public abstract void onTemplateChanged();

  @Override
  public Lifecycle getLifecycle() {
    return mLifecycleRegistry;
  }

  @Override
  public boolean handlesTemplateChangeAnimation() {
    return false;
  }

  @Override
  public boolean isFullScreen() {
    return true;
  }

  @Override
  public boolean usesSurface() {
    return false;
  }

  /**
   * Restores the presenter's focus to the last focused view.
   *
   * <p>Note: A bug in GMS core causes {@link View#isInTouchMode()} to return {@code true} even in
   * rotary or touchpad mode (b/128031459). When {@link View#layout(int, int, int, int)} is called,
   * the focus is cleared if {@link View#isInTouchMode()} returns {@code true}. Because the correct
   * touch mode value is eventually set, we can work around this issue by setting the {@link
   * #mLastFocusedView} in when the focus changes and restoring the focus when the touch mode is
   * {@code false} in a {@link OnTouchModeChangeListener}.
   *
   * <p>We call {@link #setLastFocusedView(View)} in these places:
   *
   * <ul>
   *   <li>In {@link #setDefaultFocus()}: after the presenter is created.
   *   <li>In {@link #setTemplate(TemplateWrapper)}: when the presenter is updated.
   *   <li>In {@link #mOnGlobalFocusChangeListener}: when the user moves the focus in the presenter.
   * </ul>
   */
  @VisibleForTesting
  public void restoreFocus() {
    View view = mLastFocusedView;
    if (view != null) {
      view.requestFocus();
    }
  }

  /**
   * Moves focus to one of the {@code toViews} if the focus is present in one of the {@code
   * fromViews}.
   *
   * <p>The focus will move to the first view in {@code toViews} that can take focus.
   *
   * @return {@code true} if the focus has been moved, otherwise {@code false}
   */
  protected static boolean moveFocusIfPresent(List<View> fromViews, List<View> toViews) {
    for (View fromView : fromViews) {
      if (fromView.hasFocus()) {
        for (View toView : toViews) {
          if (toView.getVisibility() == VISIBLE && toView.requestFocus()) {
            return true;
          }
        }
        return false;
      }
    }
    return false;
  }

  /** Returns whether the window containing the presenter's view has focus. */
  protected boolean hasWindowFocus() {
    if (mHasWindowFocusOverride != null) {
      return mHasWindowFocusOverride;
    }

    return getView().hasWindowFocus();
  }

  /** Returns the view that should get focus by default. */
  protected View getDefaultFocusedView() {
    return getView();
  }

  /**
   * Sets the presenter's last focused view.
   *
   * @see #restoreFocus() for details on how we work around a focus-related GMS core bug
   */
  private void setLastFocusedView(View focusedView) {
    mLastFocusedView = focusedView;
  }

  private static void sendColorContrastTelemetryEvent(
      TemplateContext templateContext, String templateClassName) {
    TelemetryHandler telemetryHandler = templateContext.getTelemetryHandler();
    telemetryHandler.logCarAppTelemetry(
        TelemetryEvent.newBuilder(
                templateContext.getColorContrastCheckState().getCheckPassed()
                    ? UiAction.COLOR_CONTRAST_CHECK_PASSED
                    : UiAction.COLOR_CONTRAST_CHECK_FAILED,
                templateContext.getCarAppPackageInfo().getComponentName())
            .setTemplateClassName(templateClassName));

    // Reset color contrast check state
    templateContext.getColorContrastCheckState().setCheckPassed(true);
  }
}
