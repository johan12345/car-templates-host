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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.Pair;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;

/** Utility class to load a car app's primary and secondary colors. */
public final class ColorUtils {
  private static final String KEY_THEME = "androidx.car.app.theme";

  // LINT.IfChange(car_colors)
  public static final String KEY_COLOR_PRIMARY = "carColorPrimary";
  public static final String KEY_COLOR_PRIMARY_DARK = "carColorPrimaryDark";
  public static final String KEY_COLOR_SECONDARY = "carColorSecondary";
  public static final String KEY_COLOR_SECONDARY_DARK = "carColorSecondaryDark";
  // LINT.ThenChange()

  private ColorUtils() {}

  /** Returns a {@link Context} set up for the given package. */
  @Nullable
  public static Context getPackageContext(Context context, String packageName) {
    Context packageContext;
    try {
      packageContext = context.createPackageContext(packageName, /* flags= */ 0);
    } catch (PackageManager.NameNotFoundException e) {
      L.e(LogTags.APP_HOST, e, "Package %s does not exist", packageName);
      return null;
    }
    return packageContext;
  }

  /**
   * Returns the ID of the theme to use for the app described by the given component name.
   *
   * <p>This theme id is used to load custom primary and secondary colors from the remote app.
   *
   * @see com.google.android.libraries.car.app.model.CarColor
   */
  @StyleRes
  public static int loadThemeId(Context context, ComponentName componentName) {
    int theme = 0;
    ServiceInfo serviceInfo = getServiceInfo(context, componentName);
    if (serviceInfo != null && serviceInfo.metaData != null) {
      theme = serviceInfo.metaData.getInt(KEY_THEME);
    }

    // If theme is not specified in service information, fallback to KEY_THEME in application
    // info.
    if (theme == 0) {
      ApplicationInfo applicationInfo = getApplicationInfo(context, componentName);
      if (applicationInfo != null) {
        if (applicationInfo.metaData != null) {
          theme = applicationInfo.metaData.getInt(KEY_THEME);
        }
        // If no override provided in service and application info, fallback to default app
        // theme.
        if (theme == 0) {
          theme = applicationInfo.theme;
        }
      }
    }

    return theme;
  }

  /**
   * Returns the color values for the given light and dark variants.
   *
   * <p>If a variant is not specified in the theme, default values are returned for both variants.
   */
  public static Pair<Integer, Integer> getColorVariants(
      Resources.Theme appTheme,
      String packageName,
      String colorKey,
      String darkColorKey,
      @ColorInt int defaultColor,
      @ColorInt int defaultDarkColor) {
    Resources appResources = appTheme.getResources();
    int colorId = appResources.getIdentifier(colorKey, "attr", packageName);
    int darkColorId = appResources.getIdentifier(darkColorKey, "attr", packageName);

    // If light or dark variant is not specified, return default variants.
    if (colorId == Resources.ID_NULL || darkColorId == Resources.ID_NULL) {
      return new Pair<>(defaultColor, defaultDarkColor);
    }

    @ColorInt int color = getColor(colorId, /* defaultColor= */ Color.TRANSPARENT, appTheme);
    @ColorInt
    int darkColor = getColor(darkColorId, /* defaultColor= */ Color.TRANSPARENT, appTheme);

    // Even if the resource ID exists for a variant, it may not have a value. If so, use default
    // variants.
    if (color == Color.TRANSPARENT || darkColor == Color.TRANSPARENT) {
      return new Pair<>(defaultColor, defaultDarkColor);
    }
    return new Pair<>(color, darkColor);
  }

  /** Returns the color specified by the given resource id from the given app theme. */
  @ColorInt
  private static int getColor(int resId, @ColorInt int defaultColor, Resources.Theme appTheme) {
    @ColorInt int color = defaultColor;
    if (resId != Resources.ID_NULL) {
      int[] attr = {resId};
      TypedArray ta = appTheme.obtainStyledAttributes(attr);
      color = ta.getColor(0, defaultColor);
      ta.recycle();
    }
    return color;
  }

  @Nullable
  private static ServiceInfo getServiceInfo(Context context, ComponentName componentName) {
    try {
      return context
          .getPackageManager()
          .getServiceInfo(componentName, PackageManager.GET_META_DATA);
    } catch (PackageManager.NameNotFoundException e) {
      L.e(LogTags.APP_HOST, e, "Component %s doesn't exist", componentName);
    }

    return null;
  }

  @Nullable
  private static ApplicationInfo getApplicationInfo(Context context, ComponentName componentName) {
    try {
      return context
          .getPackageManager()
          .getApplicationInfo(componentName.getPackageName(), PackageManager.GET_META_DATA);
    } catch (PackageManager.NameNotFoundException e) {
      L.e(LogTags.APP_HOST, e, "Package %s doesn't exist", componentName.getPackageName());
    }

    return null;
  }
}
