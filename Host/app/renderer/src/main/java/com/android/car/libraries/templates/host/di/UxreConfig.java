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
package com.android.car.libraries.templates.host.di;

/** An interface used for providing UXRE configs */
public interface UxreConfig {

  /** The max size of a car app template stack */
  int getTemplateStackMaxSize(int defaultValue);

  /** The max length of a car app list for showing routes. */
  int getRouteListMaxLength(int defaultValue);

  /** The max length of a car app list for showing pane information. */
  int getPaneMaxLength(int defaultValue);

  /** The max length of a car app grid view. */
  int getGridMaxLength(int defaultValue);

  /**
   * The max length of a generic, uniform car app list for cases where the OEM did not override the
   * default UXRE cumulative content limit value.
   */
  int getListMaxLength(int defaultValue);

  /** Default max string length */
  int getCarAppDefaultMaxStringLength(int defaultValue);
}
