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

/**
 * Manages the state of color contrast checks in template apps.
 *
 * <p>This class tracks the state for a single template in a single app.
 */
public interface ColorContrastCheckState {
  /** Sets whether the color contrast check passed in the current template. */
  void setCheckPassed(boolean passed);

  /** Returns whether the color contrast check passed in the current template. */
  boolean getCheckPassed();

  /** Returns whether the host checks color contrast. */
  // TODO(b/208683313): Remove once color contrast check is enabled in AAP
  boolean checksContrast();
}
