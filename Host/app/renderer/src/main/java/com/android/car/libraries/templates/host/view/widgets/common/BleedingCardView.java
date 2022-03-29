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

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.templates.host.R;
import java.util.Arrays;

/**
 * A card view that "bleeds" through the bottom of its parent.
 *
 * <p>"Bleeding" means its rounded corners become square at the bottom when the card's bottom is at,
 * or past its parent's bottom, thus creating an effect as if the card is "bleeding through" (or
 * "peeking out of") the bottom of the parent.
 */
public class BleedingCardView extends FrameLayout {
  // Percentage of the length of the card radius that the background radius is reduced by to avoid
  // it showing up from underneath the foreground border and creating a subtle but ugly aliasing
  // effect
  private static final float BACKGROUND_RADIUS_PERCENTAGE = 0.25f;

  private final int mRadius;
  private final int mBorderWidth;
  @ColorInt private final int mBorderColor;
  @ColorInt private int mBackgroundColor;
  private final float mWidthFraction;
  private final int mMinWidth;
  private final int mMaxWidth;
  private final int mOemWidth;
  private final int mOemMaxWidth;

  public BleedingCardView(Context context) {
    this(context, null);
  }

  public BleedingCardView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public BleedingCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  @SuppressWarnings({"ResourceType", "nullness:method.invocation", "nullness:argument"})
  public BleedingCardView(
      Context context, @Nullable AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
    super(context, attrs, defStyleAttrs, defStyleRes);

    TypedArray ta =
        context.obtainStyledAttributes(attrs, R.styleable.BleedingCardView, defStyleAttrs, 0);
    mBorderWidth = ta.getDimensionPixelSize(R.styleable.BleedingCardView_cardBorderWidth, 0);
    mBorderColor = ta.getColor(R.styleable.BleedingCardView_cardBorderColor, 0);
    @ColorInt
    int backgroundColor = ta.getColor(R.styleable.BleedingCardView_cardBackgroundColor, 0);
    @ColorInt int textColor = ta.getColor(R.styleable.BleedingCardView_cardTextColor, 0);
    @ColorInt
    int fallbackDarkBackgroundColor =
        ta.getColor(R.styleable.BleedingCardView_cardFallbackDarkBackgroundColor, 0);
    @ColorInt
    int fallbackLightBackgroundColor =
        ta.getColor(R.styleable.BleedingCardView_cardFallbackLightBackgroundColor, 0);
    mRadius = ta.getDimensionPixelSize(R.styleable.BleedingCardView_cardRadius, 0);
    mWidthFraction = ta.getFloat(R.styleable.BleedingCardView_cardWidthFraction, 0.f);
    mMinWidth = ta.getDimensionPixelSize(R.styleable.BleedingCardView_cardMinWidth, 0);
    mMaxWidth = ta.getDimensionPixelSize(R.styleable.BleedingCardView_cardMaxWidth, 0);
    mOemWidth = ta.getDimensionPixelSize(R.styleable.BleedingCardView_cardOemWidth, 0);
    mOemMaxWidth = ta.getDimensionPixelSize(R.styleable.BleedingCardView_cardOemMaxWidth, 0);
    ta.recycle();

    setClipToOutline(true);

    mBackgroundColor =
        calculateBackgroundColor(
            backgroundColor, textColor, fallbackDarkBackgroundColor, fallbackLightBackgroundColor);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    updateCardBackground();
  }

  public int getCardRadius() {
    return mRadius;
  }

  @ColorInt
  public int getCardBackgroundColor() {
    return mBackgroundColor;
  }

  /** Sets the background color and triggers an update if it has changed. */
  public void setCardBackgroundColor(@ColorInt int backgroundColor) {
    if (mBackgroundColor == backgroundColor) {
      return;
    }
    mBackgroundColor = backgroundColor;
    updateCardBackground();
  }

  /** Sets the card width either based on the set {@link #mOemWidth}, or {@link #mWidthFraction}. */
  private void setCardWidthIfNeeded() {
    // TODO(b/162419749): Set the percent width in the xml file, without using ConstraintLayout.
    if (mOemWidth > 0) {
      // If the OEM defined the card width, use it after checking for min and max values.
      int cardWidth = mOemWidth;
      cardWidth = min(cardWidth, mOemMaxWidth);
      cardWidth = max(cardWidth, mMinWidth);
      getLayoutParams().width = cardWidth;
    } else if (mWidthFraction > 0) {
      // If the width fraction is set, use it after checking for min and max values.
      int screenWidth = getResources().getDisplayMetrics().widthPixels;
      int cardWidth = (int) (screenWidth * mWidthFraction);
      cardWidth = min(cardWidth, mMaxWidth);
      cardWidth = max(cardWidth, mMinWidth);
      getLayoutParams().width = cardWidth;
    }
  }

  @Override
  public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
    updateCardBackground();
  }

  /** Returns a background color with proper contrast ratio for the given text color. */
  @ColorInt
  private int calculateBackgroundColor(
      @ColorInt int backgroundColor,
      @ColorInt int textColor,
      @ColorInt int fallbackDarkBackgroundColor,
      @ColorInt int fallbackLightBackgroundColor) {
    if (CarColorUtils.hasMinimumColorContrast(textColor, backgroundColor)) {
      return backgroundColor;
    } else if (CarColorUtils.hasMinimumColorContrast(textColor, fallbackDarkBackgroundColor)) {
      return fallbackDarkBackgroundColor;
    } else {
      return fallbackLightBackgroundColor;
    }
  }

  private Drawable createBackground(float[] radii) {
    // Create a drawable for the background.
    GradientDrawable backDrawable = new GradientDrawable();

    // Reduce the radius a bit to avoid the background popping from outside of the border.
    float reduction = mRadius * BACKGROUND_RADIUS_PERCENTAGE;
    float[] backRadii = Arrays.copyOf(radii, 8);
    for (int i = 0; i < radii.length; ++i) {
      radii[i] -= reduction;
    }
    backDrawable.setCornerRadii(backRadii);
    backDrawable.setColor(mBackgroundColor);

    return backDrawable;
  }

  private Drawable createForeground(float[] radii) {
    // Create the border drawable.
    GradientDrawable borderDrawable = new GradientDrawable();

    // Blend the border with the background color. This method returns a fully opaque color. We
    // do
    // this instead of drawing the border over the background with an alpha so that any contents
    // of the card get are drawn underneath the border (e.g. the lighter rectangle we display
    // over
    // the lanes image) don't get blended with the border, and the border is rather of a single
    // color.
    borderDrawable.setStroke(
        mBorderWidth, CarColorUtils.blendColorsSrc(mBorderColor, mBackgroundColor));
    borderDrawable.setCornerRadii(radii);
    return borderDrawable;
  }

  private void updateCardBackground() {
    setCardWidthIfNeeded();

    // Determine whether the card is bleeding, i.e. if it goes past the bottom of the parent.
    boolean isBleeding = isBleeding();

    // Remove the bottom rounded corners if the card is bleeding.
    float bottomRadius = isBleeding ? 0 : mRadius;
    float[] radii =
        new float[] {
          mRadius, mRadius, mRadius, mRadius, bottomRadius, bottomRadius, bottomRadius, bottomRadius
        };

    // Set the background.
    setBackground(createBackground(radii));

    // Set the foreground border.
    Drawable foreground = createForeground(radii);
    if (isBleeding) {
      // If bleeding, inset the bottom with a negative value to hide the bottom border.
      foreground = new InsetDrawable(foreground, 0, 0, 0, -mBorderWidth);
    }
    setForeground(foreground);

    // Set the card view's outline with rounded corners to clip its child views (e.g. junction
    // image).
    ViewOutlineProvider outlineProvider =
        new ViewOutlineProvider() {
          @Override
          public void getOutline(View view, Outline outline) {
            int bottom = view.getHeight();
            if (isBleeding()) {
              // If the card view is bleeding, add the radius value so that only the
              // top corners are rounded.
              bottom += mRadius;
            }
            outline.setRoundRect(0, 0, view.getWidth(), bottom, mRadius);
          }
        };
    setOutlineProvider(outlineProvider);

    invalidate();
  }

  private boolean isBleeding() {
    ViewGroup parent = (ViewGroup) getParent();
    boolean isBleeding = false;
    if (parent != null) {
      int parentHeight = parent.getHeight();
      int bottom = getTop() + getHeight();
      if (bottom >= parentHeight) {
        isBleeding = true;
      }
    }
    return isBleeding;
  }
}
