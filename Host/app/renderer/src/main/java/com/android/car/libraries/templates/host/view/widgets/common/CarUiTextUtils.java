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

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarText;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.view.common.CarTextParams;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.ui.CarUiText;
import java.util.ArrayList;
import java.util.List;

/** Util class for {@link CarUiText}. */
public class CarUiTextUtils {
  private CarUiTextUtils() {}

  /** Creates a {@link CarUiText} from a {@link CarText}. */
  public static CarUiText fromCarText(
      TemplateContext context, @Nullable CarText carText, int maxLines) {
    return fromCarText(context, carText, CarTextParams.DEFAULT, maxLines);
  }

  /** Creates a {@link CarUiText} from a {@link CarText}. */
  public static CarUiText fromCarText(
      TemplateContext context, @Nullable CarText carText, CarTextParams params, int maxLines) {
    if (CarText.isNullOrEmpty(carText)) {
      return new CarUiText("", maxLines);
    }
    requireNonNull(carText);

    List<CharSequence> textVariants = new ArrayList<>();
    textVariants.add(CarTextUtils.toCharSequenceOrEmpty(context, carText, params));
    for (int i = 0; i < carText.getVariants().size(); i++) {
      textVariants.add(CarTextUtils.toCharSequenceOrEmpty(context, carText, params, i));
    }
    return new CarUiText.Builder(textVariants)
        .setMaxLines(maxLines)
        .setMaxChars(context.getConstraintsProvider().getStringCharacterLimit())
        .build();
  }

  /** Creates a {@link CarUiText} from a {@link CharSequence}. */
  public static CarUiText fromCharSequence(
      TemplateContext context, @NonNull CharSequence charSequence, int maxLines) {

    return new CarUiText.Builder(charSequence)
        .setMaxLines(maxLines)
        .setMaxChars(context.getConstraintsProvider().getStringCharacterLimit())
        .build();
  }
}
