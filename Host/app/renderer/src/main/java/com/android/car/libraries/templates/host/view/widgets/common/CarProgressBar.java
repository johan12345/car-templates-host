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
import android.widget.ProgressBar;
import androidx.annotation.Nullable;
import com.android.car.libraries.templates.host.R;

/** An {@link ProgressBar} that enforces size limits on OEM-customized width and height. */
public class CarProgressBar extends ProgressBar {
  private final int mMinSize;
  private final int mMaxSize;

  public CarProgressBar(Context context) {
    this(context, null);
  }

  public CarProgressBar(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CarProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public CarProgressBar(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    TypedArray ta =
        context.obtainStyledAttributes(attrs, R.styleable.CarProgressBar, defStyleAttr, 0);
    mMinSize = ta.getDimensionPixelSize(R.styleable.CarProgressBar_imageMinSize, 0);
    mMaxSize = ta.getDimensionPixelSize(R.styleable.CarProgressBar_imageMaxSize, Integer.MAX_VALUE);
    ta.recycle();
  }

  @Override
  protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Set the OEM-customizable image size, with the min and max limits.
    ViewUtils.enforceViewSizeLimit(this, mMinSize, mMaxSize, mMinSize, mMaxSize);

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }
}
