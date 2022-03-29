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
package com.android.car.libraries.templates.host.view.presenters.common;

import android.content.Context;
import android.content.res.Resources;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;

/** Assorted presenter utilities. */
public abstract class PresenterUtils {
  /**
   * Applies the top window insets of the root view of a template to the {@code viewContainer}.
   *
   * <p>This is needed for templates that use a overlaid view on a background surface, so that the
   * status bar is rendered above the surface, and the view container is moved down so that it is
   * not drawn under the status bar text.
   */
  public static void applyTopWindowInsetsToContainer(int topInset, ViewGroup viewContainer) {
    ViewGroup.LayoutParams layoutParams = viewContainer.getLayoutParams();
    if (layoutParams instanceof MarginLayoutParams) {
      ((MarginLayoutParams) layoutParams).topMargin = topInset;
      viewContainer.setLayoutParams(layoutParams);
    }
  }

  /**
   * Returns the margin value to be applied on the left and right side to set a view's width to be a
   * fraction of the screen width.
   */
  public static int getAdaptiveMargin(Context context, float containerWidthFraction) {
    Resources resources = context.getResources();
    if (resources == null) {
      return 0;
    }
    int screenWidth = resources.getDisplayMetrics().widthPixels;

    float marginFraction = (1.f - containerWidthFraction) / 2;
    return (int) (screenWidth * marginFraction);
  }

  private PresenterUtils() {}
}
