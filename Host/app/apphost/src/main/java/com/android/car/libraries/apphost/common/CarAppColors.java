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

package com.android.car.libraries.apphost.common;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.ColorInt;

/** A container class for a car app's primary and secondary colors. */
public class CarAppColors {
  @ColorInt public final int primaryColor;
  @ColorInt public final int primaryDarkColor;
  @ColorInt public final int secondaryColor;
  @ColorInt public final int secondaryDarkColor;

  /** Constructs an instance of {@link CarAppColors}. */
  public CarAppColors(
      int primaryColor, int primaryDarkColor, int secondaryColor, int secondaryDarkColor) {
    this.primaryColor = primaryColor;
    this.primaryDarkColor = primaryDarkColor;
    this.secondaryColor = secondaryColor;
    this.secondaryDarkColor = secondaryDarkColor;
  }

  /** Returns a default {@link CarAppColors} to use, based on the host's default colors. */
  public static CarAppColors getDefault(Context context, HostResourceIds hostResourceIds) {
    Resources resources = context.getResources();
    return new CarAppColors(
        resources.getColor(hostResourceIds.getDefaultPrimaryColor()),
        resources.getColor(hostResourceIds.getDefaultPrimaryDarkColor()),
        resources.getColor(hostResourceIds.getDefaultSecondaryColor()),
        resources.getColor(hostResourceIds.getDefaultSecondaryDarkColor()));
  }
}
