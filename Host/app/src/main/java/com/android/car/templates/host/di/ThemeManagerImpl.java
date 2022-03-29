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
package com.android.car.templates.host.di;

import android.content.Context;
import com.android.car.libraries.templates.host.R;
import com.android.car.libraries.templates.host.di.ThemeManager;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ServiceComponent;

/** The service level module to provide {@link ThemeManager}. */
@Module
@InstallIn(ServiceComponent.class)
class ThemeManagerImpl implements ThemeManager {

  @Provides
  static ThemeManager provideThemeManager() {
    return new ThemeManagerImpl();
  }

  private ThemeManagerImpl() {}

  /** Applies appropriate theme to the given context. */
  @Override
  public void applyTheme(Context context) {
    context.getTheme().applyStyle(R.style.Theme_Template, true);
  }
}
