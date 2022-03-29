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

import static com.android.car.libraries.apphost.common.CarHostConfig.PRIMARY_ACTION_HORIZONTAL_ORDER_LEFT;
import static com.android.car.libraries.apphost.common.CarHostConfig.PRIMARY_ACTION_HORIZONTAL_ORDER_NOT_SET;
import static com.android.car.libraries.apphost.common.CarHostConfig.PRIMARY_ACTION_HORIZONTAL_ORDER_RIGHT;
import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.CarHostConfig.PrimaryActionOrdering;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.common.ActionButtonListParams;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.ActionButtonView.ActionFlag;
import java.util.ArrayList;
import java.util.List;

/** Displays a list of {@link Action}s as buttons in a single horizontal layout. */
public class ActionButtonListView extends LinearLayout {

  public enum Gravity {
    /* Indicates that action button list can be rendered within the content. */
    CENTER,

    /* Indicates that action button list should be pinned to the bottom of the content. */
    BOTTOM
  }

  /** A flag that indicates whether the buttons stretch horizontally to fill the available space. */
  private final boolean mButtonsStretch;

  /**
   * The maximum button width.
   *
   * <p>This limit is only applied when {@link #mButtonsStretch} is set to {@code true}.
   */
  private final int mButtonMaxWidth;

  /** The horizontal spacing between the button list and the parent view. */
  private final int mHorizontalSpacing;

  /** Minimum touch area for each button in this list */
  private final int mMinTouchTargetSize;

  @ColorInt private final int mDefaultButtonBackgroundColor;

  public ActionButtonListView(Context context) {
    this(context, null);
  }

  public ActionButtonListView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ActionButtonListView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public ActionButtonListView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateActionButtonListButtonStretchHorizontal,
      R.attr.templateActionButtonListButtonMaxWidth,
      R.attr.templatePlainContentHorizontalPadding,
      R.attr.templateActionButtonTouchTargetSize,
      R.attr.templateActionButtonDefaultBackgroundColor,
    };

    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mButtonsStretch = ta.getBoolean(0, false);
    mButtonMaxWidth = ta.getDimensionPixelSize(1, 0);
    mHorizontalSpacing = ta.getDimensionPixelSize(2, 0);
    mMinTouchTargetSize = ta.getDimensionPixelSize(3, 0);
    mDefaultButtonBackgroundColor = ta.getColor(4, 0);
    ta.recycle();
  }

  /** Returns the {@link ActionButtonView} for testing. */
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public ActionButtonView getActionButtonView(int index) {
    int maxIndex = getChildCount() - 1;
    if (index > maxIndex || index < 0) {
      throw new IndexOutOfBoundsException(
          "Action index is not within bounds of [0, " + maxIndex + "]");
    }

    View child = getChildAt(index);
    if (child instanceof ActionButtonView) {
      return (ActionButtonView) child;
    }
    throw new IllegalStateException(
        "Found unexpected type of view in action list: " + child.getClass());
  }

  /** Returns the size of the list for testing. */
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public int size() {
    return getChildCount();
  }

  /**
   * Sets the {@link Action}s that will be mapped into buttons.
   *
   * @see ActionFlag
   */
  public void setActionList(
      TemplateContext templateContext, List<Action> actionList, ActionButtonListParams params) {
    LayoutInflater inflater = LayoutInflater.from(getContext());

    removeAllViews();

    if (actionList == null || actionList.isEmpty()) {
      this.setVisibility(View.GONE);
      return;
    }

    setVisibility(View.VISIBLE);

    @StyleableRes final int[] themeAttrs = {R.attr.templateActionButtonMargin};
    TypedArray ta = templateContext.obtainStyledAttributes(themeAttrs);
    int actionMargin = ta.getDimensionPixelOffset(0, 0);
    ta.recycle();

    int maxActions = params.getMaxActions();
    if (actionList.size() > maxActions) {
      L.w(
          LogTags.TEMPLATE,
          "The number of actions exceeds the maximum allowed action count, skipping later actions");
      actionList = actionList.subList(0, maxActions);
    }

    if (params.allowOemReordering()) {
      int primaryActionOrder = templateContext.getCarHostConfig().getPrimaryActionOrder();
      actionList = reorderActionList(actionList, primaryActionOrder);
    }

    // Calculate the stretching button width by subtracting the margins and paddings from the
    // screen width, and dividing the remaining width by the button count. Then cap the
    // resulting width at the button max width.
    int screenWidth = getResources().getDisplayMetrics().widthPixels;
    int buttonCount = actionList.size();
    int stretchingButtonWidth =
        min(
            (screenWidth - actionMargin * (buttonCount - 1) - 2 * mHorizontalSpacing) / buttonCount,
            mButtonMaxWidth);

    ViewGroup touchContainer = (ViewGroup) getParent();
    touchContainer.setTouchDelegate(null);

    boolean allowAppColor =
        getAllowAppColor(templateContext, actionList, params, mDefaultButtonBackgroundColor);
    params = ActionButtonListParams.builder(params).setAllowAppColor(allowAppColor).build();

    int count = 0;
    for (Action action : actionList) {
      View view = inflater.inflate(R.layout.action_button_view, this, false);
      ((ActionButtonView) view).setAction(templateContext, action, params);

      LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
      if (count > 0) {
        layoutParams.setMarginStart(actionMargin);
      }

      if (mButtonsStretch) {
        layoutParams.width = stretchingButtonWidth;
      }

      addView(view, layoutParams);
      ViewUtils.setMinTapTarget(touchContainer, view, mMinTouchTargetSize);
      ++count;
    }
  }

  /** Returns whether app-provided colors can be applied to the buttons. */
  private static boolean getAllowAppColor(
      TemplateContext templateContext,
      List<Action> actionList,
      ActionButtonListParams params,
      @ColorInt int defaultButtonBackgroundColor) {
    if (templateContext.getCarHostConfig().isButtonColorOverriddenByOEM()
        && params.allowOemColorOverride()) {
      // OEM overrides app colors
      return false;
    }

    // Allow app colors only if the contrast check passes
    return checkColorContrast(
        templateContext, actionList, params.getSurroundingColor(), defaultButtonBackgroundColor);
  }

  /**
   * Checks the color contrast between contents of the given action list and the background color.
   */
  private static boolean checkColorContrast(
      TemplateContext templateContext,
      List<Action> actionList,
      @ColorInt int surroundingColor,
      @ColorInt int defaultButtonBackgroundColor) {
    for (Action action : actionList) {
      // Check if the background color has enough contrast against the surrounding color.
      CarColor backgroundCarColor = action.getBackgroundColor();
      if (backgroundCarColor != null) {
        if (!CarColorUtils.checkColorContrast(
            templateContext, backgroundCarColor, surroundingColor)) {
          return false;
        }
      }

      // Check if the text color has enough contrast against the background color.
      @ColorInt
      int backgroundColor =
          ActionButtonViewUtils.getBackgroundColor(
              templateContext,
              action,
              /* surroundingColor= */ surroundingColor,
              /* defaultBackgroundColor= */ defaultButtonBackgroundColor);
      CarText title = action.getTitle();
      if (title != null) {
        if (!CarTextUtils.checkColorContrast(templateContext, title, backgroundColor)) {
          return false;
        }
      }

      // Check if the icon tint has enough contrast against the background color.
      CarIcon icon = action.getIcon();
      if (icon != null) {
        CarColor tint = icon.getTint();
        if (tint != null) {
          if (!CarColorUtils.checkColorContrast(templateContext, tint, backgroundColor)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /** {@link ActionButtonView}s will carry out the action when clicked on without toast. */
  public void enableActionButtons() {
    for (int index = 0; index < getChildCount(); index++) {
      View child = getChildAt(index);
      if (child instanceof ActionButtonView) {
        ActionButtonView button = (ActionButtonView) child;
        button.enableActionButton();
      }
    }
  }

  /**
   * {@link ActionButtonView}s will show a toast with given message instead of carrying out the
   * action when clicked on.
   */
  public void disableActionButtons(String disabledToastMessage) {
    for (int index = 0; index < getChildCount(); index++) {
      View child = getChildAt(index);
      if (child instanceof ActionButtonView) {
        ActionButtonView button = (ActionButtonView) child;
        button.disableActionButton(disabledToastMessage);
      }
    }
  }

  private List<Action> reorderActionList(
      List<Action> actionList, @PrimaryActionOrdering int primaryActionOrder) {
    ArrayList<Action> mutableActionList = new ArrayList<>(actionList);
    if (primaryActionOrder == PRIMARY_ACTION_HORIZONTAL_ORDER_NOT_SET) {
      return actionList;
    }
    int indexOfPrimaryAction = 0;
    @Nullable Action primaryAction = null;
    for (Action action : mutableActionList) {
      if (ActionButtonViewUtils.isPrimaryAction(action)) {
        primaryAction = action;
        break;
      }
      indexOfPrimaryAction++;
    }
    if (primaryAction != null) {
      mutableActionList.remove(indexOfPrimaryAction);
      if (primaryActionOrder == PRIMARY_ACTION_HORIZONTAL_ORDER_LEFT) {
        mutableActionList.add(0, primaryAction);
      } else if (primaryActionOrder == PRIMARY_ACTION_HORIZONTAL_ORDER_RIGHT) {
        mutableActionList.add(primaryAction);
      }
    }
    return mutableActionList;
  }
}
