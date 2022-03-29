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
import androidx.annotation.ColorInt;

/** Encapsulates parameters that configure the way action button list instances are rendered. */
public class ActionButtonListParams {

  private final int mMaxActions;
  private final boolean mAllowOemReordering;
  private final boolean mAllowOemColorOverride;
  private final boolean mAllowAppColor;
  @ColorInt private final int mSurroundingColor;

  /** Returns a builder of {@link ActionButtonListParams}. */
  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(ActionButtonListParams params) {
    return new Builder()
        .setMaxActions(params.getMaxActions())
        .setOemReorderingAllowed(params.allowOemReordering())
        .setOemColorOverrideAllowed(params.allowOemColorOverride())
        .setAllowAppColor(params.allowAppColor())
        .setSurroundingColor(params.getSurroundingColor());
  }

  /** Returns the maximum number of action buttons in the list. */
  public int getMaxActions() {
    return mMaxActions;
  }
  /**
   * Returns the surrounding color against which the button will be displayed.
   *
   * <p>This color is used to compare the contrast between the surrounding color and the button
   * background color.
   *
   * @see Builder#setSurroundingColor(int)
   */
  @ColorInt
  public int getSurroundingColor() {
    return mSurroundingColor;
  }

  /** Returns whether the button can have app-defined colors. */
  public boolean allowAppColor() {
    return mAllowAppColor;
  }

  /** Returns whether the buttons can be re-ordered by OEMs or not. */
  public boolean allowOemReordering() {
    return mAllowOemReordering;
  }

  /** Returns whether the button colors can be overridden by OEMs. */
  public boolean allowOemColorOverride() {
    return mAllowOemColorOverride;
  }

  private ActionButtonListParams(
      int maxActions,
      boolean allowOemReordering,
      boolean allowOemColorOverride,
      boolean allowAppColor,
      @ColorInt int surroundingColor) {
    mMaxActions = maxActions;
    mAllowOemReordering = allowOemReordering;
    mAllowOemColorOverride = allowOemColorOverride;
    mAllowAppColor = allowAppColor;
    mSurroundingColor = surroundingColor;
  }

  /** A builder of {@link ActionButtonListParams} instances. */
  public static class Builder {
    private int mMaxActions = 0;
    private boolean mAllowOemReordering = false;
    private boolean mAllowOemColorOverride = false;
    private boolean mAllowAppColor = false;
    @ColorInt private int mSurroundingColor = Color.TRANSPARENT;

    /** Sets the maximum number of action buttons in the list. */
    public Builder setMaxActions(int maxActions) {
      mMaxActions = maxActions;
      return this;
    }

    /** Sets whether the buttons can be re-ordered by OEMs or not. */
    public Builder setOemReorderingAllowed(boolean allowOemReordering) {
      mAllowOemReordering = allowOemReordering;
      return this;
    }

    /** Sets whether the button colors can be overridden by OEMs. */
    public Builder setOemColorOverrideAllowed(boolean allowOemColorOverride) {
      mAllowOemColorOverride = allowOemColorOverride;
      return this;
    }

    /** Sets whether the button can have app-defined colors. */
    public Builder setAllowAppColor(boolean allowAppColor) {
      mAllowAppColor = allowAppColor;
      return this;
    }

    /**
     * Sets the surrounding color against which the button will be displayed.
     *
     * <p>This color is used to compare the contrast between the surrounding color and the button
     * background color.
     *
     * <p>By default, the surrounding color is assumed to be transparent.
     */
    public Builder setSurroundingColor(@ColorInt int surroundingColor) {
      mSurroundingColor = surroundingColor;
      return this;
    }

    /** Constructs a {@link ActionButtonListParams} instance defined by this builder. */
    public ActionButtonListParams build() {
      return new ActionButtonListParams(
          mMaxActions,
          mAllowOemReordering,
          mAllowOemColorOverride,
          mAllowAppColor,
          mSurroundingColor);
    }
  }
}
