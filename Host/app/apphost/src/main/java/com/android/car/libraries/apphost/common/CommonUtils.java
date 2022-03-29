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

import android.content.res.Configuration;
import android.widget.Toast;
import androidx.car.app.model.OnClickDelegate;

/** Holds static util methods for common usage in the host. */
public final class CommonUtils {

  /**
   * Checks if {@code onClickDelegate} is a parked only action and the car is driving, then shows a
   * toast and returns. Otherwise dispatches the {@code onClick} to the client.
   */
  public static void dispatchClick(
      TemplateContext templateContext, OnClickDelegate onClickDelegate) {
    if (onClickDelegate.isParkedOnly()
        && templateContext.getConstraintsProvider().isConfigRestricted()) {
      templateContext
          .getToastController()
          .showToast(
              templateContext
                  .getResources()
                  .getString(templateContext.getHostResourceIds().getParkedOnlyActionText()),
              Toast.LENGTH_SHORT);
      return;
    }
    templateContext.getAppDispatcher().dispatchClick(onClickDelegate);
  }

  /** Returns {@code true} if the host is in dark mode, {@code false} otherwise. */
  public static boolean isDarkMode(TemplateContext templateContext) {
    Configuration configuration = templateContext.getResources().getConfiguration();
    return (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
        == Configuration.UI_MODE_NIGHT_YES;
  }

  private CommonUtils() {}
}
