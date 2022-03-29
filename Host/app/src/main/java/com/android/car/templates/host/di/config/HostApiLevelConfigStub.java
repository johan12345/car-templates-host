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
package com.android.car.templates.host.di.config;

import android.content.ComponentName;
import com.android.car.libraries.templates.host.di.HostApiLevelConfig;

/** Stub implementation of {@link HostApiLevelConfig} that just works as a pass-through */
final class HostApiLevelConfigStub implements HostApiLevelConfig {

  @Override
  public int getHostMinApiLevel(int defaultValue, ComponentName componentName) {
    return defaultValue;
  }

  @Override
  public int getHostMaxApiLevel(int defaultValue, ComponentName componentName) {
    return defaultValue;
  }
}
