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
package com.android.car.libraries.templates.host.view.widgets.common.testing;

import static android.view.View.VISIBLE;

import android.text.Spanned;
import android.text.SpannedString;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Switch;
import androidx.annotation.Nullable;
import com.android.car.libraries.templates.host.R;
import com.android.car.ui.widget.CarUiTextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Test helper for rows of {@code RowListView}. */
public class RowViewHelper {
  private final View mRowView;

  public RowViewHelper(View rowView) {
    mRowView = rowView;
  }

  /** Returns the title for the row as a {@link String}. */
  @Nullable
  public String getTitle() {
    CharSequence title = getTitleCharSequence();
    return title == null ? null : title.toString();
  }

  /** Get the title for the row as the direct {@link CharSequence}. */
  @Nullable
  public CharSequence getTitleCharSequence() {
    return ((CarUiTextView) mRowView.findViewById(R.id.car_ui_list_item_title)).getText();
  }

  /** Get a specified span in the title for a row. */
  @Nullable
  public <T> T getTitleSpanAt(int spanIndex, Class<T> clazz) {
    CharSequence charSequence = getTitleCharSequence();
    if (charSequence == null) {
      return null;
    }
    return getSpanAt(charSequence, spanIndex, clazz);
  }

  /** Returns {@code true} if the radio button for the row is selected. */
  public boolean isRadioButtonSelected() {
    RadioButton radioButton = mRowView.findViewById(R.id.car_ui_list_item_radio_button_widget);
    return radioButton.isChecked();
  }

  /** Returns the lines for the body rows. */
  @Nullable
  public List<String> getTextLines() {
    CarUiTextView bodyTextView = getBodyTextView();
    if (bodyTextView == null) {
      return null;
    }

    String[] lines = bodyTextView.getText().toString().split("\n");
    return Arrays.asList(lines);
  }

  /** Returns the specified span within the row text. */
  @Nullable
  public <T> T getTextSpanAt(int textIndex, int spanIndex, Class<T> clazz) {
    CharSequence text = getTextAt(textIndex);
    if (text != null) {
      return getSpanAt(text, spanIndex, clazz);
    }
    return null;
  }

  /** Returns a specific the text for a particular view index within the row. */
  @Nullable
  public CharSequence getTextAt(int index) {
    return getBodyTextLine(index);
  }

  /** Returns the max number of lines for a given view within the row. */
  public int getTextMaxLinesAt(int index) {
    CarUiTextView carUiTextView = getBodyTextView();
    return carUiTextView == null ? -1 : carUiTextView.getMaxLines();
  }

  /** Returns the image of the caret for the row. */
  @Nullable
  public ImageView getCaret() {
    return getImageView(R.id.car_ui_list_item_supplemental_icon);
  }

  /** Returns the secondary text view for the row if visible. */
  @Nullable
  public CarUiTextView getBodyTextView() {
    CarUiTextView bodyTextView = mRowView.findViewById(R.id.car_ui_list_item_body);
    if (bodyTextView.getVisibility() == VISIBLE) {
      return bodyTextView;
    }
    return null;
  }

  /** Returns the radio button for the row if visible. */
  @Nullable
  public RadioButton getRadioButton() {
    RadioButton radioButton = mRowView.findViewById(R.id.car_ui_list_item_radio_button_widget);
    if (radioButton.getVisibility() == VISIBLE) {
      return radioButton;
    }
    return null;
  }

  /** Returns the image for the row if visible. */
  @Nullable
  public ImageView getImage() {
    return getImageView(R.id.car_ui_list_item_icon);
  }

  /** Returns the {@link Switch} view for the row. */
  @Nullable
  public Switch getToggle() {
    Switch toggle = mRowView.findViewById(R.id.car_ui_list_item_switch_widget);
    if (toggle.getVisibility() == VISIBLE) {
      return toggle;
    }
    return null;
  }

  /** Returns the view containing the row elements. */
  public View getContainer() {
    return mRowView;
  }

  /** Returns the view that acts as a touch interceptor. */
  public View getTouchInterceptor() {
    return mRowView.findViewById(R.id.car_ui_list_item_touch_interceptor);
  }

  @Nullable
  private ImageView getImageView(int id) {
    ImageView imageView = mRowView.findViewById(id);
    if (imageView.getVisibility() == VISIBLE) {
      return imageView;
    }
    return null;
  }

  @Nullable
  private CharSequence getBodyTextLine(int index) {
    CarUiTextView bodyTextView = getBodyTextView();
    if (bodyTextView == null) {
      return null;
    }
    CharSequence bodyText = bodyTextView.getText();
    CharSequence[] lines = split(bodyText, "\n");
    return lines[index];
  }

  @Nullable
  private <T> T getSpanAt(CharSequence charSequence, int spanIndex, Class<T> clazz) {
    SpannedString ss = (SpannedString) charSequence;
    T[] spans = ss.getSpans(0, charSequence.length(), clazz);
    if (spans == null || spanIndex > spans.length - 1) {
      return null;
    }
    return spans[spanIndex];
  }

  private static CharSequence[] split(CharSequence charSequence, String regex) {
    // A short-cut for non-spanned strings.
    if (!(charSequence instanceof Spanned)) {
      return charSequence.toString().split(regex);
    }

    // Hereafter, emulate String.split for CharSequence.
    ArrayList<CharSequence> sequences = new ArrayList<>();
    Matcher matcher = Pattern.compile(regex).matcher(charSequence);
    int nextStart = 0;
    boolean matched = false;
    while (matcher.find()) {
      sequences.add(charSequence.subSequence(nextStart, matcher.start()));
      nextStart = matcher.end();
      matched = true;
    }
    if (!matched) {
      return new CharSequence[] {charSequence};
    }
    sequences.add(charSequence.subSequence(nextStart, charSequence.length()));
    return sequences.toArray(new CharSequence[0]);
  }
}
