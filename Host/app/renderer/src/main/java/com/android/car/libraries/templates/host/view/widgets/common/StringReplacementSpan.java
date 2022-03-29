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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.text.style.ReplacementSpan;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * A simple class for replacement the text that this span is attached to with the given replacement.
 */
public class StringReplacementSpan extends ReplacementSpan {

  private final String mReplacementText;

  public StringReplacementSpan(String text) {
    mReplacementText = text;
  }

  /** Returns the replacement string for replacing the attached text. */
  @VisibleForTesting
  public String getReplacementText() {
    return mReplacementText;
  }

  @Override
  public int getSize(
      Paint paint, CharSequence text, int start, int end, @Nullable FontMetricsInt fm) {
    Rect bounds = new Rect();
    paint.getTextBounds(mReplacementText, 0, mReplacementText.length(), bounds);
    return bounds.width();
  }

  @Override
  public void draw(
      Canvas canvas,
      CharSequence text,
      int start,
      int end,
      float x,
      int top,
      int y,
      int bottom,
      Paint paint) {
    canvas.drawText(mReplacementText, x, y, paint);
  }
}
