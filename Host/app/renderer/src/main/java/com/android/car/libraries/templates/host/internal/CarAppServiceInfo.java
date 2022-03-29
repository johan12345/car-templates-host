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
package com.android.car.libraries.templates.host.internal;

import static android.content.pm.PackageManager.GET_RESOLVED_FILTER;
import static androidx.car.app.CarAppService.CATEGORY_NAVIGATION_APP;
import static androidx.car.app.CarAppService.SERVICE_INTERFACE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/** This class provides information about car app services. */
public class CarAppServiceInfo {
  private final PackageManager mPackageManager;
  private final ComponentName mServiceName;

  public CarAppServiceInfo(Context context, ComponentName serviceName) {
    mPackageManager = context.getPackageManager();
    mServiceName = serviceName;
  }

  /** Returns true for navigation services. */
  public boolean isNavigationService() {
    Intent intent =
        new Intent(SERVICE_INTERFACE)
            .setPackage(mServiceName.getPackageName())
            .addCategory(CATEGORY_NAVIGATION_APP);
    return !mPackageManager.queryIntentServices(intent, GET_RESOLVED_FILTER).isEmpty();
  }
}
