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

import androidx.annotation.ColorInt;
import androidx.car.app.model.Action;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;

/** Util class for {@link ActionButtonView}. */
final class ActionButtonViewUtils {

  /** Returns whether the given action is a primary action. */
  static boolean isPrimaryAction(Action action) {
    return (action.getFlags() & Action.FLAG_PRIMARY) != 0;
  }

  /** Returns the background color of the given action. */
  static int getBackgroundColor(
      TemplateContext templateContext,
      Action action,
      @ColorInt int surroundingColor,
      @ColorInt int defaultBackgroundColor) {
    return CarColorUtils.resolveColor(
        templateContext,
        /* carColor= */ action.getBackgroundColor(),
        /* isDark= */ true,
        /* defaultColor= */ defaultBackgroundColor,
        /* constraints= */ CarColorConstraints.UNCONSTRAINED,
        /* backgroundColor= */ surroundingColor);
  }

  private ActionButtonViewUtils() {}
}
