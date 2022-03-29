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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.android.car.libraries.templates.host.R;

/** An {@link ImageView} that enforces size limits on OEM-customized width and height. */
@SuppressLint("AppCompatCustomView")
public final class CarImageView extends ImageView {
  private final int mMinWidth;
  private final int mMaxWidth;
  private final int mMinHeight;
  private final int mMaxHeight;

  public CarImageView(Context context) {
    this(context, null);
  }

  public CarImageView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CarImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public CarImageView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    TypedArray ta =
        context.obtainStyledAttributes(attrs, R.styleable.CarImageView, defStyleAttr, 0);
    mMinWidth = ta.getDimensionPixelSize(R.styleable.CarImageView_imageMinWidth, 0);
    mMaxWidth = ta.getDimensionPixelSize(R.styleable.CarImageView_imageMaxWidth, Integer.MAX_VALUE);
    mMinHeight = ta.getDimensionPixelSize(R.styleable.CarImageView_imageMinHeight, 0);
    mMaxHeight =
        ta.getDimensionPixelSize(R.styleable.CarImageView_imageMaxHeight, Integer.MAX_VALUE);
    ta.recycle();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // Set the OEM-customizable image size, with the min and max limits.
    ViewUtils.enforceViewSizeLimit(this, mMinWidth, mMaxWidth, mMinHeight, mMaxHeight);

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }
}
