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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import androidx.car.app.model.signin.PinSignInMethod;
import com.android.car.libraries.templates.host.R;
import com.android.car.ui.CarUiText;
import com.android.car.ui.widget.CarUiTextView;

/** A view that displays {@link PinSignInMethod} UI. */
public class PinSignInView extends FrameLayout {
  private final int mMaxWidth;

  private CarUiTextView mPinTextView;

  public PinSignInView(Context context) {
    this(context, null);
  }

  public PinSignInView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PinSignInView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @SuppressWarnings("ResourceType")
  public PinSignInView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateSignInMethodViewMaxWidth,
    };

    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    mMaxWidth = ta.getDimensionPixelSize(0, 0);
    ta.recycle();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
    if (mMaxWidth > 0 && mMaxWidth < measuredWidth) {
      int measureMode = MeasureSpec.getMode(widthMeasureSpec);
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, measureMode);
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    mPinTextView = findViewById(R.id.pin_text);
  }
  /** Returns the maximum height of mPinTextView */
  public int getMaxLines() {
    return mPinTextView.getMaxLines();
  }

  /** Sets the PIN text. */
  public void setText(CarUiText pinText) {
    mPinTextView.setText(pinText);
  }
}
