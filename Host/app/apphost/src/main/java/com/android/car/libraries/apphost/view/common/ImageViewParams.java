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

import static android.graphics.Color.TRANSPARENT;

import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarIcon;
import com.android.car.libraries.apphost.distraction.constraints.CarIconConstraints;

/** Encapsulates parameters that configure the way image view instances are rendered. */
public final class ImageViewParams {
  /** Callback for events related to image loading. */
  public interface ImageLoadCallback {
    /** Notifies that the load of the image failed. */
    void onLoadFailed(@Nullable Throwable e);

    /** Notifies that the images was successfully loaded. */
    void onImageReady();
  }

  public static final ImageViewParams DEFAULT = ImageViewParams.builder().build();

  @ColorInt private final int mDefaultTint;
  private final boolean mForceTinting;
  private final boolean mIgnoreAppTint;
  private final boolean mIsDark;
  private final CarIconConstraints mConstraints;
  @Nullable private final Drawable mPlaceholderDrawable;
  @Nullable private final ImageLoadCallback mImageLoadCallback;
  @ColorInt private final int mBackgroundColor;

  /**
   * Returns the default tint color to apply to the image if one is not specified explicitly.
   *
   * @see Builder#setDefaultTint(int)
   */
  @ColorInt
  public int getDefaultTint() {
    return mDefaultTint;
  }

  /**
   * Returns whether the default tint will be used when a {@link CarIcon} does not specify a tint.
   *
   * @see Builder#setForceTinting(boolean)
   */
  public boolean getForceTinting() {
    return mForceTinting;
  }

  /** Returns whether the app-provided tint should be ignored. */
  public boolean ignoreAppTint() {
    return mIgnoreAppTint;
  }

  /**
   * Returns whether to use the dark-variant of the tint color if one is provided.
   *
   * @see Builder#setIsDark(boolean)
   */
  public boolean getIsDark() {
    return mIsDark;
  }

  /**
   * Returns the {@link CarIconConstraints} to enforce when loading the image.
   *
   * @see Builder#setCarIconConstraints(CarIconConstraints)
   */
  public CarIconConstraints getConstraints() {
    return mConstraints;
  }

  /**
   * Returns the placeholder drawable to show while the image is loading or {@code null} to not show
   * a placeholder image.
   *
   * @see Builder#setPlaceholderDrawable(Drawable)
   */
  @Nullable
  public Drawable getPlaceholderDrawable() {
    return mPlaceholderDrawable;
  }

  /**
   * Returns the callback called when the image loading succeeds or fails or {@code null} if one is
   * not set.
   *
   * @see Builder#setImageLoadCallback(ImageLoadCallback)
   */
  @Nullable
  public ImageLoadCallback getImageLoadCallback() {
    return mImageLoadCallback;
  }

  /**
   * Sets the background color against which the text will be displayed.
   *
   * <p>This color is used only for the color contrast check, and will not be applied on the text
   * background.
   *
   * <p>By default, the background color is assumed to be transparent.
   */
  @ColorInt
  public int getBackgroundColor() {
    return mBackgroundColor;
  }

  /** Returns a builder of {@link ImageViewParams}. */
  public static Builder builder() {
    return new Builder();
  }

  private ImageViewParams(
      @ColorInt int defaultTint,
      boolean forceTinting,
      boolean isDark,
      CarIconConstraints constraints,
      @Nullable Drawable placeholderDrawable,
      @Nullable ImageLoadCallback imageLoadCallback,
      boolean ignoreAppTint,
      @ColorInt int backgroundColor) {
    mDefaultTint = defaultTint;
    mForceTinting = forceTinting;
    mIsDark = isDark;
    mConstraints = constraints;
    mPlaceholderDrawable = placeholderDrawable;
    mImageLoadCallback = imageLoadCallback;
    mIgnoreAppTint = ignoreAppTint;
    mBackgroundColor = backgroundColor;
  }

  /** A builder of {@link ImageViewParams} instances. */
  public static class Builder {
    @ColorInt private int mDefaultTint = TRANSPARENT;
    private boolean mForceTinting;
    private boolean mIgnoreAppTint;
    private boolean mIsDark;
    private CarIconConstraints mConstraints = CarIconConstraints.DEFAULT;
    @Nullable private Drawable mPlaceholderDrawable;
    @Nullable private ImageLoadCallback mImageLoadCallback;
    @ColorInt private int mBackgroundColor = TRANSPARENT;

    /**
     * Sets the tint to use by default.
     *
     * <p>If not set, the initial value is {@code TRANSPARENT}.
     *
     * <p>The default tint is used if a {@link CarIcon}'s tint is {@link
     * androidx.car.app.model.CarColor#DEFAULT}, or the icon does not specify a tint and {@code
     * #setForceTinting(true)} is called.
     */
    public Builder setDefaultTint(@ColorInt int defaultTint) {
      mDefaultTint = defaultTint;
      return this;
    }

    /**
     * Determines if the default tint will be used when a {@link CarIcon} does not specify a tint.
     *
     * <p>The default value is {@code false}.
     *
     * @see {@link #setDefaultTint(int)} for details on when the default tint is used
     */
    public Builder setForceTinting(boolean forceTinting) {
      mForceTinting = forceTinting;
      return this;
    }

    /** Determines if the app-provided icon tint should be ignored. */
    public Builder setIgnoreAppTint(boolean ignoreAppTint) {
      mIgnoreAppTint = ignoreAppTint;
      return this;
    }

    /**
     * Sets whether to use the dark-variant of the tint color if one is provided.
     *
     * <p>The default value is {@code false}.
     */
    public Builder setIsDark(boolean isDark) {
      mIsDark = isDark;
      return this;
    }

    /**
     * Sets the {@link CarIconConstraints} to enforce when loading the image.
     *
     * <p>The default value is {@link CarIconConstraints#DEFAULT}.
     */
    public Builder setCarIconConstraints(CarIconConstraints constraints) {
      mConstraints = constraints;
      return this;
    }

    /**
     * Sets the placeholder drawable to show while the image is loading.
     *
     * <p>The placeholder does not show for synchronously loaded images.
     */
    public Builder setPlaceholderDrawable(@Nullable Drawable placeholderDrawable) {
      mPlaceholderDrawable = placeholderDrawable;
      return this;
    }

    /**
     * Sets a callback called when the image loading succeeds or fails.
     *
     * <p>The callback is ignored for synchronously loaded images.
     */
    public Builder setImageLoadCallback(@Nullable ImageLoadCallback imageLoadCallback) {
      mImageLoadCallback = imageLoadCallback;
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

    /** Constructs a {@link ImageViewParams} instance defined by this builder. */
    public ImageViewParams build() {
      return new ImageViewParams(
          mDefaultTint,
          mForceTinting,
          mIsDark,
          mConstraints,
          mPlaceholderDrawable,
          mImageLoadCallback,
          mIgnoreAppTint,
          mBackgroundColor);
    }
  }
}
