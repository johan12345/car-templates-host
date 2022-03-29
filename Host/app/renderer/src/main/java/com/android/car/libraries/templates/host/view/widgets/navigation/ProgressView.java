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
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.car.libraries.templates.host.R;

/** A view that displays a progress indicator. */
public class ProgressView extends LinearLayout {
  private ProgressBar mProgressBar;

  public ProgressView(@NonNull Context context) {
    this(context, null);
  }

  public ProgressView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  @SuppressWarnings({"argument.type.incompatible"})
  public ProgressView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    mProgressBar = findViewById(R.id.progress_indicator);
  }

  /** Sets the color of the progress indicator. */
  public void setColor(@ColorInt int color) {
    mProgressBar.setIndeterminateTintList(ColorStateList.valueOf(color));
  }

  /** Sets the color of the progress indicator to its default color. */
  public void setDefaultColor() {
    mProgressBar.setIndeterminateTintList(null);
  }
}
