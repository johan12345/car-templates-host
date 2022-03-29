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

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

/** Provides package information of a car app. */
public interface CarAppPackageInfo {
  /** Package and service name of the 3p car app. */
  @NonNull
  ComponentName getComponentName();

  /**
   * Returns the primary and secondary colors of the app as defined in the metadata entry for the
   * app service, or default app theme if the metadata entry is not specified.
   */
  @NonNull
  CarAppColors getAppColors();

  /** Returns whether this app info is for a navigation app. */
  boolean isNavigationApp();

  /** Returns a round app icon for the given car app. */
  @NonNull
  Drawable getRoundAppIcon();
}
