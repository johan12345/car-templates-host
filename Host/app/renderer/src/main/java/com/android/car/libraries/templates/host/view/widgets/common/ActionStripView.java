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

import static android.widget.LinearLayout.VERTICAL;
import static com.android.car.libraries.apphost.template.view.model.ActionStripWrapper.INVALID_FOCUSED_ACTION_INDEX;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarText;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.android.car.libraries.apphost.common.CarAppError;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.common.ThreadUtils;
import com.android.car.libraries.apphost.distraction.constraints.ActionsConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.template.view.model.ActionStripWrapper;
import com.android.car.libraries.apphost.template.view.model.ActionWrapper;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.ActionStripUtils.ValidationException;
import java.util.ArrayList;
import java.util.List;

/** A view that displays an action strip for the templates. */
public class ActionStripView extends FrameLayout {
  public static final long ACTIONSTRIP_ACTIVE_STATE_DURATION_MILLIS = SECONDS.toMillis(10);
  private static final int MSG_ACTIONSTRIP_ACTIVE_STATE = 1;

  /** A delegate that responds to the visibility updates due to active state changes. */
  public interface ActiveStateDelegate {
    /** Invoked when the view's visibility changes due to the active state change. */
    void onActiveStateVisibilityChanged();
  }

  private final Handler mHandler = new Handler(new HandlerCallback());

  private boolean mIsActive = true;
  private boolean mAllowTwoLines = false;
  private LinearLayout mPrimaryContainer;
  private LinearLayout mSecondaryContainer;
  private ViewGroup mTouchContainer;
  private final int mButtonMargin;
  private final int mButtonHeight;
  private final int mMinTouchTargetSize;
  private TemplateContext mTemplateContext;
  @StyleRes private final int mFabStyleResId;

  @Nullable private ActiveStateDelegate mActiveStateDelegate;

  @Nullable ComponentName mAppName;

  public ActionStripView(Context context) {
    this(context, null);
  }

  public ActionStripView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ActionStripView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @SuppressWarnings({"ResourceType"})
  public ActionStripView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateActionStripButtonMargin,
      R.attr.templateActionButtonHeight,
      R.attr.templateActionButtonTouchTargetSize,
    };

    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mButtonMargin = ta.getDimensionPixelSize(0, 0);
    mButtonHeight = ta.getDimensionPixelOffset(1, 0);
    mMinTouchTargetSize = ta.getDimensionPixelOffset(2, 0);
    ta.recycle();

    // Get the fab appearance style resource id from the view's attributes.
    TypedArray viewStyledAttributes =
        context.obtainStyledAttributes(
            attrs, R.styleable.ActionStripView, defStyleAttr, defStyleRes);
    mFabStyleResId =
        viewStyledAttributes.getResourceId(R.styleable.ActionStripView_fabAppearance, -1);
    viewStyledAttributes.recycle();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mPrimaryContainer = findViewById(R.id.action_strip_container);
    mSecondaryContainer = findViewById(R.id.action_strip_container_secondary);
    mTouchContainer = findViewById(R.id.action_strip_touch_container);
  }

  /** Returns whether the buttons are allowed to be arranged in two lines. */
  public boolean getAllowTwoLines() {
    return mAllowTwoLines;
  }

  /** Sets the {@link ActiveStateDelegate} for this action strip view. */
  public void setActiveStateDelegate(ActiveStateDelegate activeStateDelegate) {
    this.mActiveStateDelegate = activeStateDelegate;
  }

  /**
   * Updates the {@link ActionStrip} to the {@link ActionStripView}.
   *
   * @see {@link #setActionStrip(TemplateContext, ActionStrip, ActionsConstraints, boolean)}.
   */
  public void setActionStrip(
      TemplateContext templateContext,
      @Nullable ActionStrip actionStrip,
      ActionsConstraints constraints) {
    setActionStrip(templateContext, actionStrip, constraints, false);
  }

  /**
   * Updates the {@link ActionStrip} to the {@link ActionStripView}.
   *
   * @see {@link #setActionStrip(TemplateContext, ActionStrip, ActionsConstraints, boolean)}.
   */
  public void setActionStrip(
      TemplateContext templateContext,
      @Nullable ActionStrip actionStrip,
      ActionsConstraints constraints,
      boolean allowTwoLines) {
    ActionStripWrapper actionStripWrapper =
        actionStrip == null ? null : new ActionStripWrapper.Builder(actionStrip).build();
    setActionStrip(templateContext, actionStripWrapper, constraints, allowTwoLines);
  }

  /**
   * Updates the {@link ActionStrip} to the {@link ActionStripView}.
   *
   * <p>The {@link ActionStrip} will be validated against the given {@link ActionsConstraints}
   * instance. If the number of {@link Action}s in the action strip exceeds the max allowed actions
   * as specified in the constraints, the {@link Action}s beyond the allowed number will be dropped
   * from the view.
   *
   * <p>If the {@link ActionStrip} is {@code null} or if there are no {@link Action}s added to the
   * view, the action strip will be hidden.
   *
   * <p>If {@code allowTwoLines} is {@code true}, the buttons are positioned in two lines. The last
   * two actions will be in the primary container, and the rest in the secondary container.
   */
  public void setActionStrip(
      TemplateContext templateContext,
      @Nullable ActionStripWrapper actionStrip,
      ActionsConstraints constraints,
      boolean allowTwoLines) {
    mAllowTwoLines = allowTwoLines;
    mAppName = templateContext.getCarAppPackageInfo().getComponentName();
    mTemplateContext = templateContext;
    // Ensure the model satisfies the input constraints.
    try {
      ActionStripUtils.validateRequiredTypes(actionStrip, constraints);
    } catch (ValidationException exception) {
      templateContext
          .getErrorHandler()
          .showError(
              CarAppError.builder(templateContext.getCarAppPackageInfo().getComponentName())
                  .setCause(exception)
                  .build());
    }

    if (actionStrip == null) {
      setVisibility(GONE);
      return;
    }

    // Set the host-determined index of the action button to focus. Otherwise, if a button was
    // focused, get its index before removing the button views.
    int focusedActionIndex =
        actionStrip.getFocusedActionIndex() == INVALID_FOCUSED_ACTION_INDEX
            ? getCurrentFocusedActionIndex()
            : actionStrip.getFocusedActionIndex();
    mPrimaryContainer.removeAllViews();
    mSecondaryContainer.removeAllViews();

    int maxAllowedActions = constraints.getMaxActions();
    int maxAllowedCustomTitles = constraints.getMaxCustomTitles();
    List<ActionWrapper> actions = actionStrip.getActions();
    List<ActionWrapper> allowedActions = new ArrayList<>();

    for (ActionWrapper action : actions) {
      CarText title = action.get().getTitle();
      if (title != null && !title.isEmpty()) {
        if (--maxAllowedCustomTitles < 0) {
          L.w(
              LogTags.TEMPLATE,
              "Dropping actions in action strip over max custom title limit of %d",
              constraints.getMaxCustomTitles());
          break;
        }
      }

      if (--maxAllowedActions < 0) {
        L.w(
            LogTags.TEMPLATE,
            "Dropping actions in action strip over max limit of %d",
            constraints.getMaxActions());
        break;
      }

      allowedActions.add(action);
    }

    // Go through the actions in reverse, and add them to the appropriate containers. If two
    // lines are allowed, the last two actions will be in the primary container, and the rest in
    // the secondary container.
    int lastPrimaryContainerActionIndex = allowTwoLines ? max(allowedActions.size() - 2, 0) : 0;
    for (int i = allowedActions.size() - 1; i >= 0; i--) {
      ActionWrapper action = allowedActions.get(i);

      FabView fabView = new FabView(getContext(), null, 0, mFabStyleResId);
      LinearLayout container =
          i >= lastPrimaryContainerActionIndex ? mPrimaryContainer : mSecondaryContainer;
      container.addView(fabView, 0);

      // Set the action on a fab view.
      fabView.setAction(templateContext, action);
    }

    updateFabViewLayoutParams(mPrimaryContainer);
    updateFabViewLayoutParams(mSecondaryContainer);

    List<View> actionButtons = getActionButtons();
    int actionCount = actionButtons.size();
    if (actionCount < 1) {
      setVisibility(GONE);
    } else {
      // If a button was focused before, restore the focus.
      if (focusedActionIndex >= 0) {
        int indexToFocus = min(focusedActionIndex, actionCount - 1);
        actionButtons.get(indexToFocus).requestFocus();
      }

      mPrimaryContainer.setVisibility(mPrimaryContainer.getChildCount() > 0 ? VISIBLE : GONE);
      mSecondaryContainer.setVisibility(mSecondaryContainer.getChildCount() > 0 ? VISIBLE : GONE);

      // Synchronize the visibility and the FABs clickable states with the active/idle state,
      // and do not show the strip's buttons unless it is current active.
      setVisibility(mIsActive ? VISIBLE : GONE);
      setFabViewClickableState(mIsActive);
    }

    updateTouchTarget();

    ViewUtils.logCarAppTelemetry(templateContext, UiAction.ACTION_STRIP_SIZE, actionCount);
  }

  /**
   * Requests the action strip is active after the specified delay.
   *
   * <p>When {@code true}, the action strip fades in if it is not currently visible. If {@code
   * false}, the action strip fades out.
   *
   * <p>If there is a currently pending request to activate/de-activate the action strip that has
   * not been processed yet, the previous request will be cancelled and the new request will be
   * queued.
   */
  public void setActiveStateWithDelay(boolean isActive, long millis) {
    mHandler.removeMessages(MSG_ACTIONSTRIP_ACTIVE_STATE);
    Message message = mHandler.obtainMessage(MSG_ACTIONSTRIP_ACTIVE_STATE);
    message.obj = isActive;
    mHandler.sendMessageDelayed(message, millis);
  }

  /**
   * Sets whether the action strip is active.
   *
   * <p>This will immediately activate/de-activate the action strip and cancel any pending requests
   * that might have been sent via {@link #setActiveStateWithDelay}.
   */
  public void setActiveState(boolean isActive) {
    mHandler.removeMessages(MSG_ACTIONSTRIP_ACTIVE_STATE);
    setActionStateInternal(isActive);
  }

  private void setActionStateInternal(boolean isActive) {
    if (mIsActive == isActive) {
      return;
    }

    if (mTemplateContext != null) {
      ViewUtils.logCarAppTelemetry(
          mTemplateContext, isActive ? UiAction.ACTION_STRIP_SHOW : UiAction.ACTION_STRIP_HIDE);
    }

    mIsActive = isActive;

    List<Animator> animations = new ArrayList<>();
    boolean isVisible = isActive && !getActionButtons().isEmpty();
    int animResId =
        isVisible ? R.anim.fab_view_animation_fade_in : R.anim.fab_view_animation_fade_out;
    for (View actionButton : getActionButtons()) {
      Animator animation = AnimatorInflater.loadAnimator(getContext(), animResId);
      animation.setTarget(actionButton);
      animations.add(animation);
    }

    AnimatorSet animatorSet = new AnimatorSet();
    animatorSet.playTogether(animations);
    animatorSet.setInterpolator(new FastOutSlowInInterpolator());
    animatorSet.addListener(
        new AnimatorListenerAdapter() {
          @Override
          public void onAnimationStart(Animator animation) {
            // Make the Fab clickable/non-clickable as soon as the animation starts.
            // Updating this only after the animation has started prevents the user
            // clicking in the action strip area to activate the strip, and the FAB
            // responding to the same click event.
            // TODO(b/165887188): add test for this.
            setFabViewClickableState(isActive);

            if (isVisible) {
              setVisibility(VISIBLE);

              ActiveStateDelegate delegate = mActiveStateDelegate;
              if (delegate != null) {
                delegate.onActiveStateVisibilityChanged();
              }
            }
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            if (!isVisible) {
              setVisibility(GONE);

              ActiveStateDelegate delegate = mActiveStateDelegate;
              if (delegate != null) {
                delegate.onActiveStateVisibilityChanged();
              }
            }
          }
        });

    ThreadUtils.runOnMain(() -> animatorSet.start());
  }

  /**
   * Returns the index of the focused button.
   *
   * <p>If none are focused, returns {@link ActionStripWrapper#INVALID_FOCUSED_ACTION_INDEX}.
   */
  private int getCurrentFocusedActionIndex() {
    int focusedActionIndex = INVALID_FOCUSED_ACTION_INDEX;
    List<View> actionButtons = getActionButtons();

    for (int i = 0; i < actionButtons.size(); i++) {
      View fabView = actionButtons.get(i);
      if (fabView.isFocused()) {
        focusedActionIndex = i;
        break;
      }
    }

    return focusedActionIndex;
  }

  /** Gets all action button views in the action strip. */
  private List<View> getActionButtons() {
    ArrayList<View> actionButtons = new ArrayList<>();
    for (int i = 0; i < mSecondaryContainer.getChildCount(); i++) {
      actionButtons.add(mSecondaryContainer.getChildAt(i));
    }
    for (int i = 0; i < mPrimaryContainer.getChildCount(); i++) {
      actionButtons.add(mPrimaryContainer.getChildAt(i));
    }
    return actionButtons;
  }

  private void updateFabViewLayoutParams(LinearLayout container) {
    for (int i = 0; i < container.getChildCount(); i++) {
      FabView fabView = (FabView) container.getChildAt(i);

      LinearLayout.LayoutParams layoutParams =
          new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, mButtonHeight);

      // Set the margins on buttons after the first one.
      if (i > 0) {
        if (container.getOrientation() == VERTICAL) {
          layoutParams.topMargin = mButtonMargin;
        } else {
          layoutParams.leftMargin = mButtonMargin;
        }
      }

      fabView.setLayoutParams(layoutParams);
    }
  }

  private void setFabViewClickableState(boolean clickable) {
    for (View actionButton : getActionButtons()) {
      FabView view = (FabView) actionButton;
      view.setClickable(clickable);
    }
  }

  private void updateTouchTarget() {
    mTouchContainer.setTouchDelegate(null);
    for (View actionButton : getActionButtons()) {
      FabView view = (FabView) actionButton;
      ViewUtils.setMinTapTarget(mTouchContainer, view, mMinTouchTargetSize);
    }
  }

  /** A {@link Handler.Callback} for delay activate/de-activate the action strip. */
  private class HandlerCallback implements Handler.Callback {
    @Override
    public boolean handleMessage(Message msg) {
      if (msg.what == MSG_ACTIONSTRIP_ACTIVE_STATE) {
        boolean isActive = (boolean) msg.obj;
        setActionStateInternal(isActive);
      } else {
        L.w(LogTags.TEMPLATE, "Unknown message: %s", msg);
      }
      return false;
    }
  }
}
