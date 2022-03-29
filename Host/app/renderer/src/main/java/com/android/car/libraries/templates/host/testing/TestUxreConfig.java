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
package com.android.car.libraries.templates.host.testing;

import com.android.car.libraries.templates.host.di.UxreConfig;

/** A test implementation of {@link UxreConfig} */
public class TestUxreConfig implements UxreConfig {

  private static final TestUxreConfig INSTANCE = new TestUxreConfig();

  /** Returns a {@link TestUxreConfig} implementation */
  public static TestUxreConfig getInstance() {
    return INSTANCE;
  }

  @Override
  public int getTemplateStackMaxSize(int defaultValue) {
    return defaultValue;
  }

  @Override
  public int getRouteListMaxLength(int defaultValue) {
    return defaultValue;
  }

  @Override
  public int getPaneMaxLength(int defaultValue) {
    return defaultValue;
  }

  @Override
  public int getGridMaxLength(int defaultValue) {
    return defaultValue;
  }

  @Override
  public int getListMaxLength(int defaultValue) {
    return defaultValue;
  }

  @Override
  public int getCarAppDefaultMaxStringLength(int defaultValue) {
    return defaultValue;
  }
}
