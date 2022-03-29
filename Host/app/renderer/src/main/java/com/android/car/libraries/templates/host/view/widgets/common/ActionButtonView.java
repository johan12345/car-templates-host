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

import static androidx.car.app.model.Action.TYPE_BACK;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.model.OnClickDelegate;
import com.android.car.libraries.apphost.common.CommonUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.view.common.ActionButtonListParams;
import com.android.car.libraries.apphost.view.common.CarTextParams;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.libraries.templates.host.R;
import com.android.car.ui.widget.CarUiTextView;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Displays an {@link Action} as a button. */
public class ActionButtonView extends FrameLayout {
  private static final int[] BUTTON_PRIMARY =
      new int[] {R.attr.type_primary};
  private static final int[] BUTTON_CUSTOM =
      new int[] {R.attr.type_custom};
  private static final int[] BUTTON_CUSTOM_PRIMARY =
      new int[] {
        R.attr.type_custom,
        R.attr.type_primary
      };

  @IntDef(
      flag = true,
      value = {
        FLAG_SUPPORT_REORDERING_BY_OEM,
        FLAG_SUPPORT_CUSTOMIZED_COLOR_BY_OEM,
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface ActionFlag {}

  public static final int FLAG_SUPPORT_REORDERING_BY_OEM = 1 << 0;
  public static final int FLAG_SUPPORT_CUSTOMIZED_COLOR_BY_OEM = 1 << 1;

  @ColorInt private final int mDefaultBackgroundColor;
  @ColorInt private final int mDefaultIconTint;
  private final int mMinWidthWithText;
  private final int mMinWidthWithoutText;
  private final int mSideAlignmentSpacing;
  private final int mCustomMaxEms;
  private boolean mIsPrimary;
  private boolean mIsCustom;
  private boolean mIsEnabled;
  private String mDisabledToastMessage;

  /**
   * The content alignment.
   *
   * <p>The possible values are:
   *
   * <ul>
   *   <li>0: center (default)
   *   <li>1: left
   *   <li>2: right
   * </ul>
   */
  private final int mContentAlignment;

  /** A flag that indicates whether the buttons stretch horizontally to fill the available space. */
  private final boolean mButtonsStretch;

  public ActionButtonView(Context context) {
    this(context, null);
  }

  public ActionButtonView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ActionButtonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public ActionButtonView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateActionButtonDefaultBackgroundColor,
      R.attr.templateActionDefaultIconTint,
      R.attr.templateActionWithTextMinWidth,
      R.attr.templateActionWithoutTextMinWidth,
      R.attr.templateActionButtonSideAlignmentSpacing,
      R.attr.templateActionButtonListButtonContentAlignment,
      R.attr.templateActionButtonListButtonStretchHorizontal,
    };

    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mDefaultBackgroundColor = ta.getColor(0, 0);
    mDefaultIconTint = ta.getColor(1, 0);
    mMinWidthWithText = ta.getDimensionPixelSize(2, 0);
    mMinWidthWithoutText = ta.getDimensionPixelSize(3, 0);
    mSideAlignmentSpacing = ta.getDimensionPixelSize(4, 0);
    mContentAlignment = ta.getInteger(5, 0);
    mButtonsStretch = ta.getBoolean(6, false);
    ta.recycle();

    // TODO(b/184195457): remove the custom maxEms limit when we remove the limit for all
    ta =
        context.obtainStyledAttributes(
            attrs, R.styleable.ActionButtonView, defStyleAttr, defStyleRes);
    mCustomMaxEms = ta.getInt(R.styleable.ActionButtonView_textMaxEms, 0);
    ta.recycle();

    mIsEnabled = true;
  }

  /** Returns the {@link android.view.View} title for testing. */
  @Nullable
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public String getTitle() {
    CarUiTextView carUiTextView = findViewById(R.id.action_text);
    if (carUiTextView == null) {
      return null;
    }
    return carUiTextView.getText().toString();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
  }

  /** Updates the view from the {@link Action} model. */
  public ActionButtonView setAction(
      TemplateContext templateContext, Action action, ActionButtonListParams params) {
    L.v(LogTags.TEMPLATE, "Setting action view with action: %s", action);

    removeAllViews();

    final boolean allowAppColor = params.allowAppColor();

    // Set the background color
    final CarColor color = action.getBackgroundColor();
    @ColorInt int appBackgroundColor = mDefaultBackgroundColor;
    if (color != null && allowAppColor) {
      appBackgroundColor =
          ActionButtonViewUtils.getBackgroundColor(
              templateContext,
              action,
              /* surroundingColor= */ params.getSurroundingColor(),
              /* defaultBackgroundColor= */ mDefaultBackgroundColor);
    }

    final boolean useAppColors = appBackgroundColor != mDefaultBackgroundColor;
    if (useAppColors) {
      // Set the background as tint to not override the round-corner drawable with ripple effects.
      setBackgroundTintList(ColorStateList.valueOf(appBackgroundColor));
    }

    boolean useOemColor =
        templateContext.getCarHostConfig().isButtonColorOverriddenByOEM()
            && params.allowOemColorOverride();
    updateState(
        /* isCustom= */ useAppColors,
        /* isPrimary= */ useOemColor && ActionButtonViewUtils.isPrimaryAction(action));

    // Check if the title's color span has enough contrast against the background color
    final CarColorConstraints textColorConstraints;
    CarText titleText = action.getTitle();
    if (allowAppColor
        && titleText != null
        && CarTextUtils.checkColorContrast(templateContext, titleText, appBackgroundColor)) {
      textColorConstraints = CarColorConstraints.UNCONSTRAINED;
    } else {
      textColorConstraints = CarColorConstraints.NO_COLOR;
    }

    final CarTextParams carTextParams =
        CarTextParams.builder()
            .setColorSpanConstraints(textColorConstraints)
            .setBackgroundColor(appBackgroundColor)
            .build();
    CharSequence title =
        CarTextUtils.toCharSequenceOrEmpty(templateContext, titleText, carTextParams);
    CarIcon icon = ImageUtils.getIconFromAction(action);

    boolean hasTitle = title.length() > 0;

    setMinimumWidth(hasTitle ? mMinWidthWithText : mMinWidthWithoutText);

    LayoutInflater inflater = LayoutInflater.from(getContext());
    if (icon != null && hasTitle) {
      inflater.inflate(R.layout.action_button_view_icon_text, this);
    } else if (hasTitle) {
      inflater.inflate(R.layout.action_button_view_text, this);
    } else {
      inflater.inflate(R.layout.action_button_view_icon, this);
    }

    if (icon != null) {
      ImageView iconView = findViewById(R.id.action_icon);
      ImageViewParams imageViewParams =
          ImageViewParams.builder()
              .setDefaultTint(mDefaultIconTint)
              .setForceTinting(true)
              .setIgnoreAppTint(!allowAppColor)
              .setBackgroundColor(appBackgroundColor)
              .build();
      ImageUtils.setImageSrc(templateContext, icon, iconView, imageViewParams);
    }

    if (hasTitle) {
      CarUiTextView carUiTextView = findViewById(R.id.action_text);
      if (mCustomMaxEms > 0) {
        carUiTextView.setMaxEms(mCustomMaxEms);
      } else if (mButtonsStretch) {
        // If max EMS is not set and the button stretches, allow the buttons to fill all
        // available space.
        carUiTextView.setMaxWidth(Integer.MAX_VALUE);
      }

      carUiTextView.setText(
          CarUiTextUtils.fromCarText(
              templateContext, action.getTitle(), carTextParams, carUiTextView.getMaxLines()));
    }

    // Update the click listener, if one is set.
    if (action.getType() == TYPE_BACK) {
      setOnClickListener(
          v -> {
            if (!mIsEnabled) {
              showDisabledToast(templateContext);
              return;
            }

            templateContext.getBackPressedHandler().onBackPressed();
          });
    } else {
      OnClickDelegate onClickDelegate = action.getOnClickDelegate();
      if (onClickDelegate != null) {
        setOnClickListener(
            v -> {
              ViewUtils.logCarAppTelemetry(templateContext, UiAction.ACTION_BUTTON_CLICKED);

              if (!mIsEnabled) {
                showDisabledToast(templateContext);
                return;
              }

              CommonUtils.dispatchClick(templateContext, onClickDelegate);
            });
      } else {
        setOnClickListener(null);
      }
    }

    // Set the content alignment and margins
    View contentView = getChildAt(0);
    if (contentView != null) {
      FrameLayout.LayoutParams layoutParams =
          (FrameLayout.LayoutParams) contentView.getLayoutParams();
      int contentGravity = getContentGravity(mContentAlignment);
      layoutParams.gravity = contentGravity;

      // If the content is aligned to the side, use side-alignment-specific horizontal
      // margins.
      if (contentGravity != Gravity.CENTER) {
        layoutParams.leftMargin = mSideAlignmentSpacing;
        layoutParams.rightMargin = mSideAlignmentSpacing;
      }

      contentView.setLayoutParams(layoutParams);
    }

    return this;
  }

  /** {@link ActionButtonView} will carry out the action when clicked on without toast. */
  public void enableActionButton() {
    mIsEnabled = true;
  }

  /**
   * {@link ActionButtonView} will show a toast with given message instead of carrying out the
   * action when clicked on.
   */
  public void disableActionButton(String disabledToastMessage) {
    mIsEnabled = false;
    mDisabledToastMessage = disabledToastMessage;
  }

  private void showDisabledToast(TemplateContext templateContext) {
    templateContext.getToastController().showToast(mDisabledToastMessage, Toast.LENGTH_SHORT);
  }

  /** Returns {@code true} if action button is enabled. */
  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  public boolean isActionButtonEnabled() {
    return mIsEnabled;
  }

  /** Gets the gravity value that corresponds to the content alignment value. */
  private static int getContentGravity(int contentAlignment) {
    int gravity = Gravity.CENTER_VERTICAL;
    switch (contentAlignment) {
      case 1:
        gravity |= Gravity.LEFT;
        break;
      case 2:
        gravity |= Gravity.RIGHT;
        break;
      case 0: // fall-through
      default:
        gravity = Gravity.CENTER;
    }

    return gravity;
  }

  @Override
  protected int[] onCreateDrawableState(int extraSpace) {
    int[] additionalStates;
    if (mIsPrimary && mIsCustom) {
      additionalStates = BUTTON_CUSTOM_PRIMARY;
    } else if (mIsPrimary) {
      additionalStates = BUTTON_PRIMARY;
    } else if (mIsCustom) {
      additionalStates = BUTTON_CUSTOM;
    } else {
      return super.onCreateDrawableState(extraSpace);
    }
    int[] state = super.onCreateDrawableState(extraSpace + additionalStates.length);
    mergeDrawableStates(state, additionalStates);
    return state;
  }

  private void updateState(boolean isCustom, boolean isPrimary) {
    if (isCustom != mIsCustom || isPrimary != mIsPrimary) {
      mIsCustom = isCustom;
      mIsPrimary = isPrimary;
      refreshDrawableState();
    }
  }
}
