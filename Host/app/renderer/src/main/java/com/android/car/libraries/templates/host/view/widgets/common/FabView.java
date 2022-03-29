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
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.OnClickDelegate;
import com.android.car.libraries.apphost.common.CommonUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.template.view.model.ActionWrapper;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.libraries.templates.host.R;
import com.android.car.ui.widget.CarUiTextView;

/** Displays an {@link Action} as a FAB. */
// TODO(b/158142806): Merge with ActionButtonView
public class FabView extends LinearLayout {
  private Object mAction;
  private final int mMinWidthWithText;
  private final int mMinWidthWithoutText;
  @ColorInt private final int mContentColor;
  @ColorInt private final int mBackgroundColorLight;
  @ColorInt private final int mBackgroundColorDark;

  public FabView(Context context) {
    this(context, null);
  }

  public FabView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FabView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public FabView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateActionWithTextMinWidth,
      R.attr.templateActionWithoutTextMinWidth,
      R.attr.templateActionStripFabBackgroundColorLight,
      R.attr.templateActionStripFabBackgroundColorDark
    };

    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mMinWidthWithText = ta.getDimensionPixelSize(0, 0);
    mMinWidthWithoutText = ta.getDimensionPixelSize(1, 0);
    mBackgroundColorLight = ta.getColor(2, 0);
    mBackgroundColorDark = ta.getColor(3, 0);
    ta.recycle();

    ta = context.obtainStyledAttributes(defStyleRes, new int[] {R.attr.fabDefaultContentColor});
    mContentColor = ta.getColor(0, -1);
    ta.recycle();
  }

  /** Returns whether the button contains a text or not. */
  public boolean hasTitle() {
    CarUiTextView textView = findViewById(R.id.action_text);
    return textView != null && !TextUtils.isEmpty(textView.getText());
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public Object getAction() {
    return mAction;
  }

  /** Updates the view from based on the input {@code action}. */
  public void setAction(TemplateContext templateContext, ActionWrapper actionWrapper) {
    removeAllViews();
    Action action = actionWrapper.get();
    mAction = action;

    CharSequence title = CarTextUtils.toCharSequenceOrEmpty(templateContext, action.getTitle());
    CarIcon icon = ImageUtils.getIconFromAction(action);

    LayoutInflater inflater = LayoutInflater.from(getContext());

    boolean hasTitle = title.length() > 0;
    setMinimumWidth(hasTitle ? mMinWidthWithText : mMinWidthWithoutText);

    if (icon != null && hasTitle) {
      inflater.inflate(R.layout.fab_view_icon_text, this);
    } else if (hasTitle) {
      inflater.inflate(R.layout.fab_view_text, this);
    } else {
      inflater.inflate(R.layout.action_button_view_icon, this);
    }

    if (icon != null) {
      @ColorInt
      int backgroundColor =
          CommonUtils.isDarkMode(templateContext) ? mBackgroundColorDark : mBackgroundColorLight;
      ImageViewParams imageViewParams =
          ImageViewParams.builder()
              .setDefaultTint(mContentColor)
              .setForceTinting(true)
              .setBackgroundColor(backgroundColor)
              .build();

      ImageView iconView = findViewById(R.id.action_icon);
      ImageUtils.setImageSrc(templateContext, icon, iconView, imageViewParams);
    }

    // Add the text view.
    if (hasTitle) {
      CarUiTextView carUiTextView = findViewById(R.id.action_text);
      carUiTextView.setText(
          CarUiTextUtils.fromCarText(
              templateContext, action.getTitle(), carUiTextView.getMaxLines()));
      carUiTextView.setTextColor(mContentColor);
    }

    // Update the click listener, if one is set.
    if (action.getType() == TYPE_BACK) {
      setOnClickListener(v -> templateContext.getBackPressedHandler().onBackPressed());
    } else {
      OnClickDelegate onClickDelegate = action.getOnClickDelegate();
      ActionWrapper.OnClickListener hostListener = actionWrapper.getOnClickListener();
      if (onClickDelegate != null || hostListener != null) {
        setOnClickListener(
            v -> {
              if (hostListener != null) {
                hostListener.onClick();
              }

              ViewUtils.logCarAppTelemetry(templateContext, UiAction.ACTION_STRIP_FAB_CLICKED);
              if (onClickDelegate != null) {
                CommonUtils.dispatchClick(templateContext, onClickDelegate);
              }
            });
      } else {
        setOnClickListener(null);
      }
    }
  }
}
