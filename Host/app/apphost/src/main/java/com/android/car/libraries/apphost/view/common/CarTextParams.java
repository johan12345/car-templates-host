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
package com.android.car.libraries.apphost.view.common;

import android.graphics.Color;
import android.graphics.Rect;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarColor;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;

/** Encapsulates parameters that configure the way car text instances are rendered. */
public class CarTextParams {
  /** Default params which should be used for most text in all templates. */
  public static final CarTextParams DEFAULT =
      new CarTextParams(
          /* colorSpanConstraints= */ CarColorConstraints.NO_COLOR,
          /* allowClickableSpans= */ false,
          /* imageBoundingBox= */ null,
          /* maxImages= */ 0,
          // No need to pass icon tint since no images are allowed.
          /* defaultIconTint= */ Color.TRANSPARENT,
          /* backgroundColor= */ Color.TRANSPARENT,
          /* ignoreAppIconTint= */ false);

  @Nullable private final Rect mImageBoundingBox;
  private final int mMaxImages;
  @ColorInt private final int mDefaultIconTint;
  private final boolean mIgnoreAppIconTint;
  private final CarColorConstraints mColorSpanConstraints;
  private final boolean mAllowClickableSpans;
  @ColorInt private final int mBackgroundColor;

  /** Returns a builder of {@link CarTextParams}. */
  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(CarTextParams params) {
    return new Builder()
        .setColorSpanConstraints(params.getColorSpanConstraints())
        .setAllowClickableSpans(params.getAllowClickableSpans())
        .setImageBoundingBox(params.getImageBoundingBox())
        .setMaxImages(params.getMaxImages())
        .setDefaultIconTint(params.getDefaultIconTint())
        .setBackgroundColor(params.getBackgroundColor())
        .setIgnoreAppIconTint(params.ignoreAppIconTint());
  }

  /**
   * Returns the bounding box for a span image.
   *
   * <p>Images are scaled to fit within this bounding box.
   */
  @Nullable
  Rect getImageBoundingBox() {
    return mImageBoundingBox;
  }

  /** Returns the maximum number of image spans to allow in the text. */
  int getMaxImages() {
    return mMaxImages;
  }

  /** Returns the constraints on the color spans in the text. */
  CarColorConstraints getColorSpanConstraints() {
    return mColorSpanConstraints;
  }

  /** Returns whether clickable spans are allowed in the text. */
  boolean getAllowClickableSpans() {
    return mAllowClickableSpans;
  }

  /**
   * Returns the default tint color to apply to the icon if one is not specified explicitly.
   *
   * @see Builder#setDefaultIconTint(int)
   */
  @ColorInt
  int getDefaultIconTint() {
    return mDefaultIconTint;
  }

  /** Returns whether the app-provided icon tint should be ignored. */
  public boolean ignoreAppIconTint() {
    return mIgnoreAppIconTint;
  }

  /**
   * Returns the background color against which the text will be displayed.
   *
   * @see Builder#setBackgroundColor(int)
   */
  @ColorInt
  int getBackgroundColor() {
    return mBackgroundColor;
  }

  private CarTextParams(
      CarColorConstraints colorSpanConstraints,
      boolean allowClickableSpans,
      @Nullable Rect imageBoundingBox,
      int maxImages,
      @ColorInt int defaultIconTint,
      @ColorInt int backgroundColor,
      boolean ignoreAppIconTint) {
    mColorSpanConstraints = colorSpanConstraints;
    mAllowClickableSpans = allowClickableSpans;
    mImageBoundingBox = imageBoundingBox;
    mMaxImages = maxImages;
    mDefaultIconTint = defaultIconTint;
    mBackgroundColor = backgroundColor;
    mIgnoreAppIconTint = ignoreAppIconTint;
  }

  /** A builder of {@link CarTextParams} instances. */
  public static class Builder {
    private CarColorConstraints mColorSpanConstraints = CarColorConstraints.NO_COLOR;
    private boolean mAllowClickableSpans;
    @Nullable private Rect mImageBoundingBox;
    private int mMaxImages;
    @ColorInt private int mDefaultIconTint = Color.TRANSPARENT;
    @ColorInt private int mBackgroundColor = Color.TRANSPARENT;
    private boolean mIgnoreAppIconTint;

    /**
     * Sets the constraints on the color spans in the text.
     *
     * <p>By default, no color spans are allowed in the text.
     *
     * @see #getColorSpanConstraints()
     */
    public Builder setColorSpanConstraints(CarColorConstraints colorSpanConstraints) {
      mColorSpanConstraints = colorSpanConstraints;
      return this;
    }

    /**
     * Sets whether clickable spans are allowed in the text.
     *
     * <p>By default, no clickable spans are allowed in the text.
     *
     * @see #getAllowClickableSpans()
     */
    public Builder setAllowClickableSpans(boolean allowClickableSpans) {
      mAllowClickableSpans = allowClickableSpans;
      return this;
    }

    /**
     * Sets the bounding box for the image spans.
     *
     * <p>By default, no bounding box is specified.
     *
     * @see #getImageBoundingBox()
     */
    public Builder setImageBoundingBox(@Nullable Rect imageBoundingBox) {
      mImageBoundingBox = imageBoundingBox;
      return this;
    }

    /**
     * Sets the maximum number of image spans to allow for the text.
     *
     * <p>By default, no images are allowed in the text.
     *
     * @see #getMaxImages()
     */
    public Builder setMaxImages(int maxImages) {
      mMaxImages = maxImages;
      return this;
    }

    /**
     * Sets the default tint to use for the images in the span that set their tint to {@link
     * CarColor#DEFAULT}.
     *
     * <p>This tint may vary depending on where the spans are rendered, and can be specified here.
     *
     * <p>By default, this tint is transparent.
     */
    public Builder setDefaultIconTint(@ColorInt int defaultIconTint) {
      mDefaultIconTint = defaultIconTint;
      return this;
    }

    /** Determines if the app-provided icon tint should be ignored. */
    public Builder setIgnoreAppIconTint(boolean ignoreAppIconTint) {
      mIgnoreAppIconTint = ignoreAppIconTint;
      return this;
    }

    /**
     * Sets the background color against which the text will be displayed.
     *
     * <p>This color is used only for the color contrast check, and will not be applied on the text
     * background.
     *
     * <p>By default, the background color is assumed to be transparent.
     */
    public Builder setBackgroundColor(@ColorInt int backgroundColor) {
      mBackgroundColor = backgroundColor;
      return this;
    }

    /** Constructs a {@link CarTextParams} instance defined by this builder. */
    public CarTextParams build() {
      if (mImageBoundingBox == null && mMaxImages > 0) {
        throw new IllegalStateException(
            "A bounding box needs to be provided if images are allowed in the text");
      }

      return new CarTextParams(
          mColorSpanConstraints,
          mAllowClickableSpans,
          mImageBoundingBox,
          mMaxImages,
          mDefaultIconTint,
          mBackgroundColor,
          mIgnoreAppIconTint);
    }
  }
}
