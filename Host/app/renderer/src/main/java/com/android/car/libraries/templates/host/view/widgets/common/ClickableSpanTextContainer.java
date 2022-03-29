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
import android.graphics.Color;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleableRes;
import com.android.car.libraries.templates.host.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A container for a {@link TextView} that allows moving focus between clickable spans.
 *
 * <p>Only the vertical focus movement are supported.
 */
public class ClickableSpanTextContainer extends FrameLayout implements OnGlobalFocusChangeListener {
  /** An invalid span index. */
  private static final int INVALID_INDEX = -1;

  private final ForegroundColorSpan mHighlightForegroundSpan;
  private final BackgroundColorSpan mHighlightBackgroundSpan;

  private final List<ClickableSpan> mClickableSpans = new ArrayList<>();
  private int mSelectedSpanIndex = INVALID_INDEX;

  /** Indicates whether the focus moved in between spans. */
  private boolean mMovedClickableSpanFocus = false;

  private TextView mClickableSpanCarTextView;

  public ClickableSpanTextContainer(@NonNull Context context) {
    this(context, null);
  }

  public ClickableSpanTextContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ClickableSpanTextContainer(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public ClickableSpanTextContainer(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    @StyleableRes
    final int[] themeAttrs = {
      R.attr.templateClickableSpanHighlightForegroundColor,
      R.attr.templateClickableSpanHighlightBackgroundColor,
    };

    TypedArray ta = context.obtainStyledAttributes(themeAttrs);
    int highlightForegroundColor = ta.getColor(0, Color.TRANSPARENT);
    int highlightBackgroundColor = ta.getColor(1, Color.TRANSPARENT);
    ta.recycle();

    mHighlightForegroundSpan = new ForegroundColorSpan(highlightForegroundColor);
    mHighlightBackgroundSpan = new BackgroundColorSpan(highlightBackgroundColor);
  }

  /** Sets the given text for the wrapped text view. */
  public void setText(@Nullable CharSequence text) {
    mClickableSpanCarTextView.setText(text);

    mClickableSpans.clear();
    if (text instanceof Spannable) {
      Spannable spannable = (Spannable) text;
      Collections.addAll(
          mClickableSpans, spannable.getSpans(0, text.length(), ClickableSpan.class));
    }
  }

  @Override
  public void onGlobalFocusChanged(View oldFocus, View newFocus) {
    if (newFocus == null) {
      // The focus left the window, remove the link highlight.
      removeLinkHighlight();
      return;
    }

    if (oldFocus == null) {
      // The focus came back to the window, show the link highlight again if applicable.
      updateSelectedSpan();
      return;
    }

    int focusDirection = focusDirection(oldFocus, newFocus);
    if (newFocus.equals(mClickableSpanCarTextView)) {
      // The focus moved from another view to this view. Determine which clickable is
      // selected.
      if (mMovedClickableSpanFocus) {
        // The user moved focus, but we brought the focus back to this view after changing
        // the
        // selected clickable span index to simulate the clickable span focus movement. Do
        // not reset
        // the index.
        mMovedClickableSpanFocus = false;
      } else {
        if (focusDirection == FOCUS_UP) {
          // The focus moved up. Select the last span in the list.
          mSelectedSpanIndex =
              mClickableSpans.isEmpty() ? INVALID_INDEX : mClickableSpans.size() - 1;
        } else {
          // The focus moved down. Select the first span in the list.
          mSelectedSpanIndex = mClickableSpans.isEmpty() ? INVALID_INDEX : 0;
        }
      }

      updateSelectedSpan();
    } else if (oldFocus.equals(mClickableSpanCarTextView)) {
      // The focus moved from this view to another view.
      if (mSelectedSpanIndex != INVALID_INDEX) {
        if (focusDirection == FOCUS_UP && mSelectedSpanIndex > 0) {
          // Focus moved up within the span list, select an earlier span and focus on the
          // text view
          // again.
          mSelectedSpanIndex--;
          mMovedClickableSpanFocus = true;
          mClickableSpanCarTextView.requestFocus();
        } else if (focusDirection == FOCUS_DOWN
            && mSelectedSpanIndex < mClickableSpans.size() - 1) {
          // Focus moved down within the span list, select a later span and focus on the
          // text view
          // again.
          mSelectedSpanIndex++;
          mMovedClickableSpanFocus = true;
          mClickableSpanCarTextView.requestFocus();
        } else {
          // Focus moved out of the span list, remove the selected span.
          mSelectedSpanIndex = INVALID_INDEX;
          updateSelectedSpan();
        }
      }
    }
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    mClickableSpanCarTextView = findViewById(R.id.clickable_span_text_view);

    // Enable clickable spans here, setting these in the resourc1e file does not work
    mClickableSpanCarTextView.setMovementMethod(LinkMovementMethod.getInstance());
    mClickableSpanCarTextView.setHighlightColor(Color.TRANSPARENT);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    getViewTreeObserver().addOnGlobalFocusChangeListener(this);
  }

  @Override
  protected void onDetachedFromWindow() {
    getViewTreeObserver().removeOnGlobalFocusChangeListener(this);

    super.onDetachedFromWindow();
  }

  /** Updates the selected clickable span. */
  private void updateSelectedSpan() {
    Spannable spannable = (Spannable) mClickableSpanCarTextView.getText();
    if (spannable == null) {
      return;
    }

    if (mSelectedSpanIndex == INVALID_INDEX) {
      Selection.removeSelection(spannable);
      spannable.removeSpan(mHighlightForegroundSpan);
      spannable.removeSpan(mHighlightBackgroundSpan);
    } else {
      // highlight the selected span.
      ClickableSpan span = mClickableSpans.get(mSelectedSpanIndex);
      int spanStart = spannable.getSpanStart(span);
      int spanEnd = spannable.getSpanEnd(span);
      Selection.setSelection(spannable, spanStart, spanEnd);

      spannable.setSpan(
          mHighlightForegroundSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      spannable.setSpan(
          mHighlightBackgroundSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  /**
   * Removes the link highlight from the selected span.
   *
   * <p>This method only removes the visual highlight, but not the selected span.
   */
  private void removeLinkHighlight() {
    Spannable spannable = (Spannable) mClickableSpanCarTextView.getText();
    if (spannable != null) {
      spannable.removeSpan(mHighlightForegroundSpan);
      spannable.removeSpan(mHighlightBackgroundSpan);
    }
  }

  /**
   * Determines which direction the focus moved from the old to new focus.
   *
   * <p>This method only determines the vertical focus direction.
   */
  private static int focusDirection(View oldFocus, View newFocus) {
    int[] oldLocation = getViewLocationInWindow(oldFocus);
    int[] newLocation = getViewLocationInWindow(newFocus);
    int oldLocationY = oldLocation[1];
    int newLocationY = newLocation[1];
    return oldLocationY > newLocationY ? FOCUS_UP : FOCUS_DOWN;
  }

  private static int[] getViewLocationInWindow(@Nullable View view) {
    int[] location = new int[2];
    if (view != null) {
      view.getLocationInWindow(location);
    }
    return location;
  }
}
