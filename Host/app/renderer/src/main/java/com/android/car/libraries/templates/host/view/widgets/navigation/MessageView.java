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
package com.android.car.libraries.templates.host.view.widgets.navigation;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.CarUiTextUtils;
import com.android.car.ui.widget.CarUiTextView;

/** A view that displays a message with optional image and subtext. */
public class MessageView extends LinearLayout {
  private ImageView mImageView;
  private CarUiTextView mTitleView;
  private CarUiTextView mTextView;
  private int mTitleDefaultTextColor;
  private int mTextDefaultTextColor;

  public MessageView(@NonNull Context context) {
    this(context, null);
  }

  public MessageView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  @SuppressWarnings({"argument.type.incompatible"})
  public MessageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mImageView = findViewById(R.id.message_image);
    mTitleView = findViewById(R.id.message_title);
    mTextView = findViewById(R.id.message_text);

    mTitleDefaultTextColor = mTitleView.getCurrentTextColor();
    mTextDefaultTextColor = mTextView.getCurrentTextColor();
  }

  /** Sets the color of the texts in the view. */
  public void setTextColor(@ColorInt int textColor) {
    mTitleView.setTextColor(textColor);
    mTextView.setTextColor(textColor);
  }

  /** Sets the colors of the texts in the view to their default colors. */
  public void setDefaultTextColor() {
    mTitleView.setTextColor(mTitleDefaultTextColor);
    mTextView.setTextColor(mTextDefaultTextColor);
  }

  /** Sets the title, image and text content the view. */
  public void setMessage(
      TemplateContext templateContext,
      @Nullable CarIcon image,
      CarText title,
      @Nullable CarText text,
      @ColorInt int cardBackgroundColor) {
    L.v(
        LogTags.TEMPLATE,
        "Setting message view with message: %s secondary: %s image: %s",
        title,
        text,
        image);

    boolean shouldShowImage =
        ImageUtils.setImageSrc(
            templateContext,
            image,
            mImageView,
            ImageViewParams.builder()
                .setBackgroundColor(cardBackgroundColor)
                .setIgnoreAppTint(
                    !CarColorUtils.checkIconTintContrast(
                        templateContext, image, cardBackgroundColor))
                .build());
    mImageView.setVisibility(shouldShowImage ? VISIBLE : GONE);

    mTitleView.setText(
        CarUiTextUtils.fromCarText(templateContext, title, mTitleView.getMaxLines()));

    mTextView.setText(CarUiTextUtils.fromCarText(templateContext, text, mTextView.getMaxLines()));
    mTextView.setVisibility(!CarText.isNullOrEmpty(text) ? VISIBLE : GONE);
  }
}
