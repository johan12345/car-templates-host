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
package com.android.car.libraries.apphost.view.common;

import static androidx.car.app.model.Distance.UNIT_FEET;
import static androidx.car.app.model.Distance.UNIT_KILOMETERS;
import static androidx.car.app.model.Distance.UNIT_KILOMETERS_P1;
import static androidx.car.app.model.Distance.UNIT_METERS;
import static androidx.car.app.model.Distance.UNIT_MILES;
import static androidx.car.app.model.Distance.UNIT_MILES_P1;
import static androidx.car.app.model.Distance.UNIT_YARDS;

import androidx.car.app.model.Distance;
import com.android.car.libraries.apphost.common.HostResourceIds;
import com.android.car.libraries.apphost.common.TemplateContext;
import java.text.DecimalFormat;
import org.checkerframework.checker.nullness.qual.NonNull;

/** Utilities for handling {@link Distance} instances. */
public class DistanceUtils {

  private static final DecimalFormat FORMAT_OPTIONAL_TENTH = new DecimalFormat("#0.#");
  private static final DecimalFormat FORMAT_MANDATORY_TENTH = new DecimalFormat("#0.0");

  /** Converts a {@link Distance} to a display string for the UI. */
  @NonNull
  public static String convertDistanceToDisplayString(
      @NonNull TemplateContext context, @NonNull Distance distance) {
    int displayUnit = distance.getDisplayUnit();
    HostResourceIds resIds = context.getHostResourceIds();

    String formattedDistance = convertDistanceToDisplayStringNoUnit(context, distance);
    switch (displayUnit) {
      case UNIT_METERS:
        return context.getString(resIds.getDistanceInMetersStringFormat(), formattedDistance);
      case UNIT_KILOMETERS:
      case UNIT_KILOMETERS_P1:
        return context.getString(resIds.getDistanceInKilometersStringFormat(), formattedDistance);
      case UNIT_FEET:
        return context.getString(resIds.getDistanceInFeetStringFormat(), formattedDistance);
      case UNIT_MILES:
      case UNIT_MILES_P1:
        return context.getString(resIds.getDistanceInMilesStringFormat(), formattedDistance);
      case UNIT_YARDS:
        return context.getString(resIds.getDistanceInYardsStringFormat(), formattedDistance);
      default:
        throw new UnsupportedOperationException("Unsupported distance unit type: " + displayUnit);
    }
  }

  /** Converts a {@link Distance} to a display string without units. */
  @NonNull
  public static String convertDistanceToDisplayStringNoUnit(
      @NonNull TemplateContext context, @NonNull Distance distance) {
    int displayUnit = distance.getDisplayUnit();
    double displayDistance = distance.getDisplayDistance();
    DecimalFormat format =
        (displayUnit == Distance.UNIT_KILOMETERS_P1 || displayUnit == Distance.UNIT_MILES_P1)
            ? FORMAT_MANDATORY_TENTH
            : FORMAT_OPTIONAL_TENTH;
    return format.format(displayDistance);
  }

  /** Converts {@link Distance} to meters. */
  public static int getMeters(Distance distance) {
    int displayUnit = distance.getDisplayUnit();
    switch (displayUnit) {
      case UNIT_METERS:
        return (int) Math.round(distance.getDisplayDistance());
      case UNIT_KILOMETERS:
      case UNIT_KILOMETERS_P1:
        return (int) Math.round(distance.getDisplayDistance() * 1000.0d);
      case UNIT_FEET:
        return (int) Math.round(distance.getDisplayDistance() * 0.3048d);
      case UNIT_MILES:
      case UNIT_MILES_P1:
        return (int) Math.round(distance.getDisplayDistance() * 1609.34d);
      case UNIT_YARDS:
        return (int) Math.round(distance.getDisplayDistance() * 0.9144d);
      default:
        throw new UnsupportedOperationException("Unsupported distance unit type: " + displayUnit);
    }
  }

  private DistanceUtils() {}
}
