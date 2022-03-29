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

import static androidx.car.app.model.CarColor.TYPE_BLUE;
import static androidx.car.app.model.CarColor.TYPE_CUSTOM;
import static androidx.car.app.model.CarColor.TYPE_DEFAULT;
import static androidx.car.app.model.CarColor.TYPE_GREEN;
import static androidx.car.app.model.CarColor.TYPE_PRIMARY;
import static androidx.car.app.model.CarColor.TYPE_RED;
import static androidx.car.app.model.CarColor.TYPE_SECONDARY;
import static androidx.car.app.model.CarColor.TYPE_YELLOW;
import static androidx.core.graphics.ColorUtils.calculateContrast;
import static com.android.car.libraries.apphost.common.ColorUtils.KEY_COLOR_PRIMARY;
import static com.android.car.libraries.apphost.common.ColorUtils.KEY_COLOR_PRIMARY_DARK;
import static com.android.car.libraries.apphost.common.ColorUtils.KEY_COLOR_SECONDARY;
import static com.android.car.libraries.apphost.common.ColorUtils.KEY_COLOR_SECONDARY_DARK;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Pair;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import com.android.car.libraries.apphost.distraction.constraints.CarColorConstraints;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;

/** Utilities for handling {@link CarColor} instances. */
public class CarColorUtils {

  private static final double MINIMUM_COLOR_CONTRAST = 4.5;

  /**
   * Resolves a standard color to a {@link ColorInt}.
   *
   * @return the resolved color or {@code defaultColor} if the input {@code carColor} is {@code
   *     null}, does not meet the constraints, or of the type {@link CarColor#DEFAULT}
   */
  @ColorInt
  public static int resolveColor(
      TemplateContext templateContext,
      @Nullable CarColor carColor,
      boolean isDark,
      @ColorInt int defaultColor,
      CarColorConstraints constraints) {
    return resolveColor(
        templateContext, carColor, isDark, defaultColor, constraints, Color.TRANSPARENT);
  }

  /**
   * Resolves a standard color to a {@link ColorInt}.
   *
   * <p>If {@code backgroundColor} is set to {@link Color#TRANSPARENT}, the {@code carColor} will
   * not be checked for the minimum color contrast.
   *
   * @return the resolved color or {@code defaultColor} if the input {@code carColor} is {@code
   *     null}, does not meet the constraints or minimum color contrast, or of the type {@link
   *     CarColor#DEFAULT}
   */
  @ColorInt
  public static int resolveColor(
      TemplateContext templateContext,
      @Nullable CarColor carColor,
      boolean isDark,
      @ColorInt int defaultColor,
      CarColorConstraints constraints,
      @ColorInt int backgroundColor) {
    if (carColor == null) {
      return defaultColor;
    }
    try {
      constraints.validateOrThrow(carColor);
    } catch (IllegalArgumentException e) {
      L.e(LogTags.TEMPLATE, e, "Validation failed for color %s, will use default", carColor);
      return defaultColor;
    }

    CarAppPackageInfo info = templateContext.getCarAppPackageInfo();
    CarAppColors carAppColors = info.getAppColors();
    HostResourceIds hostResourceIds = templateContext.getHostResourceIds();
    return resolveColor(
        templateContext,
        isDark,
        carColor,
        carAppColors,
        hostResourceIds,
        defaultColor,
        backgroundColor);
  }

  /** Resolves a standard color to a {@link ColorInt}. */
  @ColorInt
  public static int resolveColor(
      Context context,
      boolean isDark,
      @Nullable CarColor carColor,
      CarAppColors carAppColors,
      HostResourceIds resIds,
      @ColorInt int defaultColor,
      @ColorInt int backgroundColor) {
    if (carColor == null) {
      return defaultColor;
    }
    int type = carColor.getType();
    Resources resources = context.getResources();
    switch (type) {
      case TYPE_DEFAULT:
        return defaultColor;
      case TYPE_PRIMARY:
        return getContrastCheckedColor(
            carAppColors.primaryColor,
            carAppColors.primaryDarkColor,
            backgroundColor,
            defaultColor,
            isDark);
      case TYPE_SECONDARY:
        return getContrastCheckedColor(
            carAppColors.secondaryColor,
            carAppColors.secondaryDarkColor,
            backgroundColor,
            defaultColor,
            isDark);
      case TYPE_RED:
        return resources.getColor(isDark ? resIds.getRedDarkColor() : resIds.getRedColor());
      case TYPE_GREEN:
        return resources.getColor(isDark ? resIds.getGreenDarkColor() : resIds.getGreenColor());
      case TYPE_BLUE:
        return resources.getColor(isDark ? resIds.getBlueDarkColor() : resIds.getBlueColor());
      case TYPE_YELLOW:
        return resources.getColor(isDark ? resIds.getYellowDarkColor() : resIds.getYellowColor());
      case TYPE_CUSTOM:
        return getContrastCheckedColor(
            carColor.getColor(), carColor.getColorDark(), backgroundColor, defaultColor, isDark);
      default:
        L.e(LogTags.TEMPLATE, "Failed to resolve standard color id: %d", type);
        return defaultColor;
    }
  }

  /**
   * Returns the {@link CarAppColors} from the given app name if all primary and secondary colors
   * are present in the app's manifest, otherwise returns {@link CarAppColors#getDefault(Context,
   * HostResourceIds)}.
   */
  public static CarAppColors resolveAppColor(
      @NonNull Context context,
      @NonNull ComponentName appName,
      @NonNull HostResourceIds hostResourceIds) {
    String packageName = appName.getPackageName();
    CarAppColors defaultColors = CarAppColors.getDefault(context, hostResourceIds);

    int themeId = ColorUtils.loadThemeId(context, appName);
    if (themeId == 0) {
      L.w(LogTags.TEMPLATE, "Cannot get the app theme from %s", packageName);
      return defaultColors;
    }

    Context packageContext = ColorUtils.getPackageContext(context, packageName);
    if (packageContext == null) {
      L.w(LogTags.TEMPLATE, "Cannot get the app context from %s", packageName);
      return defaultColors;
    }
    packageContext.setTheme(themeId);

    Resources.Theme theme = packageContext.getTheme();
    Pair<Integer, Integer> primaryColorVariants =
        ColorUtils.getColorVariants(
            theme,
            packageName,
            KEY_COLOR_PRIMARY,
            KEY_COLOR_PRIMARY_DARK,
            defaultColors.primaryColor,
            defaultColors.primaryDarkColor);
    Pair<Integer, Integer> secondaryColorVariants =
        ColorUtils.getColorVariants(
            theme,
            packageName,
            KEY_COLOR_SECONDARY,
            KEY_COLOR_SECONDARY_DARK,
            defaultColors.secondaryColor,
            defaultColors.secondaryDarkColor);

    return new CarAppColors(
        primaryColorVariants.first,
        primaryColorVariants.second,
        secondaryColorVariants.first,
        secondaryColorVariants.second);
  }

  /**
   * Darkens the given color by a percentage of its brightness.
   *
   * @param originalColor the color to change the brightness of
   * @param percentage the percentage to decrement the brightness for, in the [0..1] range. For
   *     example, a value of 0.5 will make the color 50% less bright
   */
  @ColorInt
  public static int darkenColor(@ColorInt int originalColor, float percentage) {
    float[] hsv = new float[3];
    Color.colorToHSV(originalColor, hsv);
    hsv[2] *= 1.f - percentage;
    return Color.HSVToColor(hsv);
  }

  /**
   * Blends two colors using a SRC Porter-duff operator.
   *
   * <p>See <a href="http://ssp.impulsetrain.com/porterduff.html">Porter-Duff Compositing and Blend
   * Modes</a>
   *
   * <p>NOTE: this function ignores the alpha channel of the destination, and returns a fully opaque
   * color.
   */
  @ColorInt
  public static int blendColorsSrc(@ColorInt int source, @ColorInt int destination) {
    // Each color component is calculated like so:
    // output_color = (1 - alpha(source)) * destination + alpha_source * source
    float alpha = Color.alpha(source) / 255.f;
    return Color.argb(
        255,
        clampComponent(alpha * Color.red(source) + (1 - alpha) * Color.red(destination)),
        clampComponent(alpha * Color.green(source) + (1 - alpha) * Color.green(destination)),
        clampComponent(alpha * Color.blue(source) + (1 - alpha) * Color.blue(destination)));
  }

  /**
   * Checks whether the given colors provide an acceptable contrast ratio.
   *
   * <p>See <a href="https://material.io/design/usability/accessibility.html#color-and-contrast">
   * Color and Contrast</a>
   *
   * <p>If {@code backgroundColor} is {@link Color#TRANSPARENT}, any {@code foregroundColor} will
   * pass the check.
   *
   * @param foregroundColor the foreground color for which the contrast should be checked.
   * @param backgroundColor the background color for which the contrast should be checked.
   * @return true if placing the foreground color over the background color results in an acceptable
   *     contrast.
   */
  public static boolean hasMinimumColorContrast(
      @ColorInt int foregroundColor, @ColorInt int backgroundColor) {
    if (backgroundColor == Color.TRANSPARENT) {
      return true;
    }

    return calculateContrast(foregroundColor, backgroundColor) > MINIMUM_COLOR_CONTRAST;
  }

  /**
   * Check if any variant in the given {@code foregroundCarColor} has enough color contrast against
   * the given {@code backgroundColor}.
   */
  public static boolean checkColorContrast(
      TemplateContext templateContext, CarColor foregroundCarColor, @ColorInt int backgroundColor) {
    if (backgroundColor == Color.TRANSPARENT) {
      return true;
    }

    if (CarColor.DEFAULT.equals(foregroundCarColor)) {
      return true;
    }

    CarColor foregroundColor = convertToCustom(templateContext, foregroundCarColor);
    boolean checkPasses =
        hasMinimumColorContrast(foregroundColor.getColor(), backgroundColor)
            || hasMinimumColorContrast(foregroundColor.getColorDark(), backgroundColor);
    if (!checkPasses) {
      L.w(
          LogTags.TEMPLATE,
          "Color contrast check failed, foreground car color: %s, background color: %d",
          foregroundCarColor,
          backgroundColor);
      templateContext.getColorContrastCheckState().setCheckPassed(false);
    }
    return checkPasses;
  }

  /**
   * Returns whether the icon's tint passes the color contrast check against the given background
   * color.
   */
  public static boolean checkIconTintContrast(
      TemplateContext templateContext, @Nullable CarIcon icon, @ColorInt int backgroundColor) {
    boolean passes = true;
    if (icon != null) {
      CarColor iconTint = icon.getTint();
      if (iconTint != null) {
        passes = checkColorContrast(templateContext, iconTint, backgroundColor);
      }
    }
    return passes;
  }

  /**
   * Convert the given {@code carColor} into a {@link CarColor} of type {@link
   * CarColor#TYPE_CUSTOM}.
   */
  private static CarColor convertToCustom(TemplateContext templateContext, CarColor carColor) {
    if (carColor.getType() == TYPE_CUSTOM) {
      return carColor;
    }

    @ColorInt
    int color =
        resolveColor(
            templateContext,
            carColor,
            /* isDark= */ false,
            Color.TRANSPARENT,
            CarColorConstraints.UNCONSTRAINED);
    @ColorInt
    int colorDark =
        resolveColor(
            templateContext,
            carColor,
            /* isDark= */ true,
            Color.TRANSPARENT,
            CarColorConstraints.UNCONSTRAINED);
    return CarColor.createCustom(color, colorDark);
  }

  /**
   * Between the given {@code color} and {@code colorDark}, returns the color that has enough color
   * contrast against the given {@code backgroundColor}.
   *
   * <p>If none of the given colors passes the check, returns {@code defaultColor}.
   *
   * <p>If {@code isDark} is {@code true}, {@code colorDark} will be checked first, otherwise {@code
   * color} will be checked first. The first color passes the check will be returned.
   */
  @ColorInt
  private static int getContrastCheckedColor(
      @ColorInt int color,
      @ColorInt int colorDark,
      @ColorInt int backgroundColor,
      @ColorInt int defaultColor,
      boolean isDark) {
    int[] colors = new int[2];
    if (isDark) {
      colors[0] = colorDark;
      colors[1] = color;
    } else {
      colors[0] = color;
      colors[1] = colorDark;
    }

    for (@ColorInt int col : colors) {
      if (hasMinimumColorContrast(col, backgroundColor)) {
        return col;
      }
    }
    return defaultColor;
  }

  private static int clampComponent(float color) {
    return (int) Math.max(0, Math.min(255, color));
  }

  private CarColorUtils() {}
}
