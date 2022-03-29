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
package com.android.car.libraries.apphost.internal;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.car.libraries.apphost.common.AppIconLoader;
import com.android.car.libraries.apphost.common.CarAppColors;
import com.android.car.libraries.apphost.common.CarAppPackageInfo;
import com.android.car.libraries.apphost.common.CarColorUtils;
import com.android.car.libraries.apphost.common.HostResourceIds;
import java.util.Objects;

/** Provides package information of a 3p car app built using AndroidX Car SDK (go/watevra). */
public class CarAppPackageInfoImpl implements CarAppPackageInfo {
  private final Context mContext;
  private final ComponentName mComponentName;
  private final boolean mIsNavigationApp;
  private final AppIconLoader mAppIconLoader;
  private final HostResourceIds mHostResourceIds;

  private boolean mIsLoaded;
  @Nullable private CarAppColors mCarAppColors;

  /**
   * Creates a {@link CarAppPackageInfoImpl} for the application identified by the given {@link
   * ComponentName}.
   *
   * @param context Host context, used to retrieve host resources and configurations
   * @param componentName Identifier of the car app this instance will provide metadata for
   * @param isNavigationApp Whether the given car app is a navigation app or not
   * @param hostResourceIds Host resources, used to retrieve default colors to use in case the app
   *     doesn't provide their own
   */
  public static CarAppPackageInfo create(
      @NonNull Context context,
      @NonNull ComponentName componentName,
      boolean isNavigationApp,
      @NonNull HostResourceIds hostResourceIds,
      @NonNull AppIconLoader appIconLoader) {
    return new CarAppPackageInfoImpl(
        context, componentName, isNavigationApp, hostResourceIds, appIconLoader);
  }

  @Override
  @NonNull
  public ComponentName getComponentName() {
    return mComponentName;
  }

  @NonNull
  @Override
  public CarAppColors getAppColors() {
    ensureLoaded();
    return Objects.requireNonNull(mCarAppColors);
  }

  @Override
  public boolean isNavigationApp() {
    return mIsNavigationApp;
  }

  @Override
  @NonNull
  public Drawable getRoundAppIcon() {
    return mAppIconLoader.getRoundAppIcon(mContext, mComponentName);
  }

  @Override
  public String toString() {
    return "[" + mComponentName.flattenToShortString() + ", isNav: " + mIsNavigationApp + "]";
  }

  @SuppressLint("ResourceType")
  private void ensureLoaded() {
    if (mIsLoaded) {
      return;
    }

    mCarAppColors = CarColorUtils.resolveAppColor(mContext, mComponentName, mHostResourceIds);
    mIsLoaded = true;
  }

  private CarAppPackageInfoImpl(
      @NonNull Context context,
      @NonNull ComponentName componentName,
      boolean isNavigationApp,
      @NonNull HostResourceIds hostResourceIds,
      @NonNull AppIconLoader appIconLoader) {
    mContext = context;
    mComponentName = componentName;
    mIsNavigationApp = isNavigationApp;
    mHostResourceIds = hostResourceIds;
    mAppIconLoader = appIconLoader;
  }
}
