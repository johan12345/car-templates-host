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

import com.android.car.libraries.templates.host.di.FeaturesConfig;
import com.android.car.libraries.templates.host.di.HostApiLevelConfig;
import com.android.car.libraries.templates.host.di.UxreConfig;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ServiceComponent;

/** The service level module to privde configs to AOSP Templates Host */
@Module
@InstallIn(ServiceComponent.class)
class StubModule {
  @Provides
  static FeaturesConfig provideFeaturesConfig() {
    return new FeaturesConfigStub();
  }

  @Provides
  static UxreConfig provideUxreConfig() {
    return new UxreConfigStub();
  }

  @Provides
  static HostApiLevelConfig provideHostApiLevelConfig() {
    return new HostApiLevelConfigStub();
  }

  private StubModule() {}
}
