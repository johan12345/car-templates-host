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
package com.android.car.libraries.apphost.distraction.checkers;

import android.content.Context;
import androidx.car.app.CarAppPermission;
import androidx.car.app.navigation.model.NavigationTemplate;

/** A {@link TemplateChecker} implementation for {@link NavigationTemplate} */
public class NavigationTemplateChecker implements TemplateChecker<NavigationTemplate> {
  @Override
  public boolean isRefresh(NavigationTemplate newTemplate, NavigationTemplate oldTemplate) {
    // Always allow routing template refreshes.
    return true;
  }

  @Override
  public void checkPermissions(Context context, NavigationTemplate newTemplate) {
    CarAppPermission.checkHasLibraryPermission(context, CarAppPermission.NAVIGATION_TEMPLATES);
  }
}
