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

package com.android.car.libraries.apphost.distraction.constraints;

/** Used to provide different limit values for the car app. */
public interface ConstraintsProvider {
  /** Provides the max length this car app can use for a content type. */
  default int getContentLimit(int contentType) {
    return 0;
  }

  /** Provides the max size for the template stack for this car app. */
  default int getTemplateStackMaxSize() {
    return 0;
  }

  /** Provides the max length this car app can use for a text view */
  default int getStringCharacterLimit() {
    return Integer.MAX_VALUE;
  }

  /** Returns true if keyboard is restricted for this car app */
  default boolean isKeyboardRestricted() {
    return false;
  }

  /** Returns true if config is restricted for this car app */
  default boolean isConfigRestricted() {
    return false;
  }

  /** Returns true if filtering is restricted for this car app */
  default boolean isFilteringRestricted() {
    return false;
  }
}
