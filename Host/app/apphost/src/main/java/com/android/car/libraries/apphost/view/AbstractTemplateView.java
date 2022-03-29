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

import static com.android.car.libraries.apphost.common.EventManager.EventType.TEMPLATE_TOUCHED_OR_FOCUSED;
import static com.android.car.libraries.apphost.common.EventManager.EventType.WINDOW_FOCUS_CHANGED;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.ViewTreeObserver.OnWindowFocusChangeListener;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Template;
import androidx.car.app.model.TemplateWrapper;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.common.ThreadUtils;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A view that displays {@link Template}s.
 *
 * <p>The current template can be set with {@link #setTemplate} method.
 */
public abstract class AbstractTemplateView extends FrameLayout {
  /**
   * The {@link TemplatePresenter} for the template currently set in the view or {@code null} if
   * none is set.
   */
  @Nullable private TemplatePresenter mCurrentPresenter;

  /** The {@link Lifecycle} object of the parent of this view (e.g. the car activity hosting it). */
  @MonotonicNonNull private Lifecycle mParentLifecycle;

  /**
   * An observer for the {@link #mParentLifecycle}, which is registered and unregistered when the
   * view is attached and detached.
   */
  @Nullable private LifecycleObserver mLifecycleObserver;

  /**
   * Context for various {@link TemplatePresenter}s to retrieve important bits of information for
   * presenting content.
   */
  @MonotonicNonNull private TemplateContext mTemplateContext;

  /** {@link WindowInsets} to apply to templates. */
  @MonotonicNonNull private WindowInsets mWindowInsets;

  /**
   * The window focus value in the last callback from the {@link OnWindowFocusChangeListener}.
   *
   * <p>We need to store this value because the listener is called even if the window focus state
   * does not change, when the view focus moves.
   */
  private boolean mLastWindowFocusState;

  /** A callback called when the template view's window focus changes. */
  private final OnWindowFocusChangeListener mOnWindowFocusChangeListener =
      new OnWindowFocusChangeListener() {
        @SuppressWarnings("nullness") // suppress under initialization warning for this
        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
          if (hasFocus != mLastWindowFocusState) {
            // Dispatch the window focus event only when the window focus state changes.
            dispatchWindowFocusEvent();
            mLastWindowFocusState = hasFocus;
          }
        }
      };

  private final OnPreDrawListener mOnPreDrawListener =
      new OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
          TemplatePresenter presenter = mCurrentPresenter;
          if (presenter != null) {
            return presenter.onPreDraw();
          }
          return true;
        }
      };

  protected AbstractTemplateView(Context context) {
    this(context, null);
  }

  protected AbstractTemplateView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  protected AbstractTemplateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /**
   * Returns the {@link SurfaceViewContainer} which holds the surface that 3p apps can use to render
   * custom content.
   */
  protected abstract SurfaceViewContainer getSurfaceViewContainer();

  /** Returns the {@link FrameLayout} container which holds the currently set template. */
  protected abstract ViewGroup getTemplateContainer();

  /**
   * Returns the minimum top padding to use when laying out the UI.
   *
   * <p>This is used to ensure there is some spacing from top of the screen to the UI when there is
   * no status bar (i.e. widescreen).
   */
  protected abstract int getMinimumTopPadding();

  /**
   * Returns a {@link TemplateTransitionManager} responsible for handling transitions between
   * presenters
   */
  protected abstract TemplateTransitionManager getTransitionManager();

  /** Returns the current {@link TemplateContext} or {@code null} if one has not been set. */
  @Nullable
  protected TemplateContext getTemplateContext() {
    return mTemplateContext;
  }

  /**
   * Returns a {@link SurfaceProvider} which can be used to retrieve the {@link
   * android.view.Surface} that 3p apps can use to draw custom content.
   */
  public SurfaceProvider getSurfaceProvider() {
    return getSurfaceViewContainer();
  }

  /**
   * Sets the parent {@link Lifecycle} for this view.
   *
   * <p>This is normally the activity or fragment the view is attached to.
   */
  public void setParentLifecycle(Lifecycle parentLifecycle) {
    mParentLifecycle = parentLifecycle;
  }

  /** Returns the parent {@link Lifecycle}. */
  protected @Nullable Lifecycle getParentLifecycle() {
    return mParentLifecycle;
  }

  /** Sets the {@link TemplateContext} for this view. */
  public void setTemplateContext(TemplateContext templateContext) {
    mTemplateContext = TemplateContext.from(templateContext, getContext());
  }

  /** Stores the window insets to apply to templates. */
  public void setWindowInsets(WindowInsets windowInsets) {
    mWindowInsets = windowInsets;
    if (mCurrentPresenter != null) {
      mCurrentPresenter.applyWindowInsets(windowInsets, getMinimumTopPadding());
    }
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
    dispatchTouchFocusEvent();
    if (mCurrentPresenter != null && mCurrentPresenter.onKeyUp(keyCode, keyEvent)) {
      return true;
    }
    return super.onKeyUp(keyCode, keyEvent);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    dispatchTouchFocusEvent();
    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onGenericMotionEvent(MotionEvent motionEvent) {
    dispatchTouchFocusEvent();
    return super.onGenericMotionEvent(motionEvent);
  }

  @Override
  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (mParentLifecycle != null) {
      initLifecycleObserver(mParentLifecycle);
    }

    ViewTreeObserver viewTreeObserver = getViewTreeObserver();
    viewTreeObserver.addOnWindowFocusChangeListener(mOnWindowFocusChangeListener);
    viewTreeObserver.addOnPreDrawListener(mOnPreDrawListener);
  }

  /** Returns the presenter currently attached to this view. */
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  @Nullable
  public TemplatePresenter getCurrentPresenter() {
    return mCurrentPresenter;
  }

  /** Sets the {@link Template} to display in the view, or {@code null} to display nothing. */
  @MainThread
  public void setTemplate(TemplateWrapper templateWrapper) {
    ThreadUtils.ensureMainThread();

    // First convert the template to another template type if needed.
    templateWrapper =
        TemplateConverterRegistry.get().maybeConvertTemplate(getContext(), templateWrapper);

    TemplatePresenter previousPresenter = mCurrentPresenter;
    if (mCurrentPresenter != null) {
      TemplatePresenter presenter = mCurrentPresenter;

      Template template = templateWrapper.getTemplate();

      // Allow the existing presenter to update the views if:
      //   1) Both the previous and the new template are  of the same class.
      //   2) The new template is a refresh OR the presenter handles the template change
      // animation.
      boolean updatePresenter = presenter.getTemplate().getClass().equals(template.getClass());
      updatePresenter &= templateWrapper.isRefresh() || presenter.handlesTemplateChangeAnimation();
      if (updatePresenter) {
        updatePresenter(presenter, templateWrapper);
        return;
      }

      // The current presenter is not of the same type as the given template, so remove it. We
      // will create a new presenter below and re-add it if needed.
      // TODO(b/151953922): Test the ordering of pause, remove view, destroy.
      pausePresenter(presenter);
      stopPresenter(presenter);
      destroyPresenter(presenter);
      mCurrentPresenter = null;
    }

    TemplatePresenter presenter = createPresenter(templateWrapper);
    mCurrentPresenter = presenter;
    transition(presenter, previousPresenter);

    if (presenter != null) {
      presenter.setDefaultFocus();
    }
  }

  private void transition(@Nullable TemplatePresenter to, @Nullable TemplatePresenter from) {
    if (to != null) {
      getTransitionManager()
          .transition(getTemplateContainer(), getSurfaceViewContainer(), to, from);
    } else {
      getSurfaceViewContainer().setVisibility(GONE);
      View previousView = from == null ? null : from.getView();
      if (previousView != null) {
        getTemplateContainer().removeView(previousView);
      }
    }
  }

  /**
   * Returns the {@link WindowInsets} to apply to the templates presented by the view or {@code
   * null} if not set.
   *
   * @see #setWindowInsets(WindowInsets)
   */
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  @Nullable
  public WindowInsets getWindowInsets() {
    return mWindowInsets;
  }

  @Override
  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public void onDetachedFromWindow() {
    ViewTreeObserver viewTreeObserver = getViewTreeObserver();
    viewTreeObserver.removeOnWindowFocusChangeListener(mOnWindowFocusChangeListener);
    viewTreeObserver.removeOnPreDrawListener(mOnPreDrawListener);

    // Stop the presenter, since its view is no longer visible.
    TemplatePresenter presenter = mCurrentPresenter;
    if (presenter != null) {
      stopPresenter(presenter);
    }

    if (mLifecycleObserver != null && mParentLifecycle != null) {
      mParentLifecycle.removeObserver(mLifecycleObserver);
      mLifecycleObserver = null;
    }

    super.onDetachedFromWindow();
  }

  /**
   * Let any listeners know that an(y) UI element within the template view has been interacted on,
   * either via touch or focus events.
   */
  private void dispatchTouchFocusEvent() {
    TemplateContext context = mTemplateContext;
    if (context != null) {
      context.getEventManager().dispatchEvent(TEMPLATE_TOUCHED_OR_FOCUSED);
    }
  }

  /**
   * Let any listeners know that the window that contains the template view has changed its focus
   * state.
   */
  private void dispatchWindowFocusEvent() {
    TemplateContext context = mTemplateContext;
    if (context != null) {
      context.getEventManager().dispatchEvent(WINDOW_FOCUS_CHANGED);
    }
  }

  /** Updates the given presenter with the data from the given template. */
  private static void updatePresenter(
      TemplatePresenter presenter, TemplateWrapper templateWrapper) {
    Preconditions.checkState(
        presenter.getTemplate().getClass().equals(templateWrapper.getTemplate().getClass()));

    L.d(LogTags.TEMPLATE, "Updating presenter: %s", presenter);
    presenter.setTemplate(templateWrapper);
  }

  /** Pauses the given presenter. */
  private static void pausePresenter(TemplatePresenter presenter) {
    L.d(LogTags.TEMPLATE, "Pausing presenter: %s", presenter);

    State currentState = presenter.getLifecycle().getCurrentState();
    if (currentState.isAtLeast(State.RESUMED)) {
      presenter.onPause();
    }
  }

  /** Stops the given presenter. */
  private static void stopPresenter(TemplatePresenter presenter) {
    L.d(LogTags.TEMPLATE, "Stopping presenter: %s", presenter);

    State currentState = presenter.getLifecycle().getCurrentState();
    if (currentState.isAtLeast(State.STARTED)) {
      presenter.onStop();
    }
  }

  /** Destroys the given presenter. */
  private static void destroyPresenter(TemplatePresenter presenter) {
    L.d(LogTags.TEMPLATE, "Destroying presenter: %s", presenter);

    presenter.onDestroy();
  }

  /**
   * Creates and starts a new presenter for the given template or {@code null} if a presenter could
   * not be found for it.
   */
  @Nullable
  private TemplatePresenter createPresenter(TemplateWrapper templateWrapper) {
    if (mTemplateContext == null) {
      throw new IllegalStateException(
          "templateContext is null when attempting to create a presenter");
    }

    TemplatePresenter presenter =
        TemplatePresenterRegistry.get().createPresenter(mTemplateContext, templateWrapper);
    if (presenter == null) {
      L.w(
          LogTags.TEMPLATE,
          "No presenter available for template type: %s",
          templateWrapper.getTemplate().getClass().getSimpleName());
      return null;
    }

    L.d(LogTags.TEMPLATE, "Creating new presenter: %s", presenter);
    presenter.onCreate();

    if (mParentLifecycle != null) {
      // Only start and resume it if our parent parent lifecycle is in those states. If not,
      // we will
      // switch to the state when/if the parent lifecycle reaches it later on.
      State parentState = mParentLifecycle.getCurrentState();
      if (parentState.isAtLeast(State.STARTED)) {
        presenter.onStart();
      }
      if (parentState.isAtLeast(State.RESUMED)) {
        presenter.onResume();
      }
    }

    if (mWindowInsets != null) {
      presenter.applyWindowInsets(mWindowInsets, getMinimumTopPadding());
    }
    return presenter;
  }

  /**
   * Instantiates a parent lifecycle observer that forwards the relevant events to the current
   * presenter.
   */
  private void initLifecycleObserver(Lifecycle parentLifecycle) {
    mLifecycleObserver =
        new DefaultLifecycleObserver() {
          @Override
          public void onStart(LifecycleOwner lifecycleOwner) {
            if (mCurrentPresenter != null) {
              mCurrentPresenter.onStart();
            }
          }

          @Override
          public void onStop(LifecycleOwner lifecycleOwner) {
            if (mCurrentPresenter != null) {
              mCurrentPresenter.onStop();
            }
          }

          @Override
          public void onResume(LifecycleOwner lifecycleOwner) {
            if (mCurrentPresenter != null) {
              mCurrentPresenter.onResume();
            }
          }

          @Override
          public void onPause(LifecycleOwner lifecycleOwner) {
            if (mCurrentPresenter != null) {
              mCurrentPresenter.onPause();
            }
          }

          @Override
          public void onDestroy(LifecycleOwner lifecycleOwner) {
            if (mCurrentPresenter != null) {
              mCurrentPresenter.onDestroy();
            }
          }
        };
    parentLifecycle.addObserver(mLifecycleObserver);
  }
}
