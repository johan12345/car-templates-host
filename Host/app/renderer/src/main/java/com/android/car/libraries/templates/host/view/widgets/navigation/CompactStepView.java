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
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarText;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.view.common.CarTextParams;
import com.android.car.libraries.apphost.view.common.CarTextUtils;
import com.android.car.libraries.apphost.view.common.ImageUtils;
import com.android.car.libraries.apphost.view.common.ImageViewParams;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.view.widgets.common.CarUiTextUtils;
import com.android.car.ui.widget.CarUiTextView;

/**
 * A view that displays a compact view of a navigation maneuver.
 *
 * <p>For example it could show just the maneuver description or a description and an icon.
 */
public class CompactStepView extends LinearLayout {
  private ImageView mTurnSymbolView;
  private CarUiTextView mDescriptionText;
  @Nullable private Step mStep;
  private int mDescriptionTextDefaultTextColor;

  public CompactStepView(Context context) {
    this(context, null);
  }

  public CompactStepView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CompactStepView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public CompactStepView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mTurnSymbolView = findViewById(R.id.compact_turn_symbol);
    mDescriptionText = findViewById(R.id.compact_description_text);

    mDescriptionTextDefaultTextColor = mDescriptionText.getCurrentTextColor();
  }

  /** Sets the color of the texts in the view. */
  public void setTextColor(@ColorInt int textColor) {
    mDescriptionText.setTextColor(textColor);
  }

  /** Sets the colors of the texts in he view to their default colors. */
  public void setDefaultTextColor() {
    mDescriptionText.setTextColor(mDescriptionTextDefaultTextColor);
  }

  /**
   * Sets the {@link Step} to be shown.
   *
   * <p>Setting a {@code null} steo will cause the view to be hidden.
   */
  public void setStep(
      TemplateContext templateContext,
      @Nullable Step step,
      CarTextParams carTextParams,
      @ColorInt int cardBackgroundColor) {
    L.v(LogTags.TEMPLATE, "Setting compact step view with step: %s", step);

    mStep = step;
    if (step == null) {
      setVisibility(GONE);
      return;
    }
    Maneuver maneuver = step.getManeuver();
    CarIcon turnIcon = maneuver == null ? null : maneuver.getIcon();
    boolean shouldShowTurnIcon =
        ImageUtils.setImageSrc(
            templateContext,
            turnIcon,
            mTurnSymbolView,
            ImageViewParams.builder()
                .setBackgroundColor(cardBackgroundColor)
                .setIgnoreAppTint(
                    !CarColorUtils.checkIconTintContrast(
                        templateContext, turnIcon, cardBackgroundColor))
                .build());
    mTurnSymbolView.setVisibility(shouldShowTurnIcon ? VISIBLE : GONE);

    CarTextParams.Builder paramsBuilder =
        CarTextParams.builder(carTextParams).setBackgroundColor(cardBackgroundColor);
    CarText cue = step.getCue();
    if (cue != null) {
      paramsBuilder.setIgnoreAppIconTint(
          !CarTextUtils.checkColorContrast(templateContext, cue, cardBackgroundColor));
    }

    mDescriptionText.setText(
        CarUiTextUtils.fromCarText(
            templateContext, cue, paramsBuilder.build(), mDescriptionText.getMaxLines()));
    setVisibility(VISIBLE);
  }

  @VisibleForTesting
  @Nullable
  public Step getStep() {
    return mStep;
  }
}
