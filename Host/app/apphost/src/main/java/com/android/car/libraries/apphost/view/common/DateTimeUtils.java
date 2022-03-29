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

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.car.app.model.DateTimeWithZone;
import com.android.car.libraries.apphost.common.HostResourceIds;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import java.text.DateFormat;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

/** Utilities for formatting and manipulating dates and times. */
@SuppressWarnings("NewApi") // java.time APIs used throughout are OK through de-sugaring.
public class DateTimeUtils {

  /** Returns a string from a duration in order to display it in the UI. */
  public static String formatDurationString(TemplateContext context, Duration duration) {
    long days = duration.toDays();
    long hours = duration.minusDays(days).toHours();
    long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
    HostResourceIds resIds = context.getHostResourceIds();

    String result = "";
    if (days > 0) {
      if (hours == 0) {
        result = context.getString(resIds.getDurationInDaysStringFormat(), days);
      } else {
        result = context.getString(resIds.getDurationInDaysAndHoursStringFormat(), days, hours);
      }
    } else if (hours > 0) {
      if (minutes == 0) {
        result = context.getString(resIds.getDurationInHoursStringFormat(), hours);
      } else {
        result =
            context.getString(resIds.getDurationInHoursAndMinutesStringFormat(), hours, minutes);
      }
    } else {
      result = context.getString(resIds.getDurationInMinutesStringFormat(), minutes);
    }

    return result;
  }

  /**
   * Returns a string to display in the UI from an arrival time at a destination that may be in a
   * different time zone than the one given by {@code currentZoneId).
   *
   * <p>If the time zone offset at the destination is not the same as the current time zone, an
   * abbreviated time zone string is added, for example "5:38 PM PST".
   */
  public static String formatArrivalTimeString(
      @NonNull TemplateContext context,
      @NonNull DateTimeWithZone timeAtDestination,
      @NonNull ZoneId currentZoneId) {
    // Get the offsets for the current and destination time zones.
    long destinationTimeUtcMillis = timeAtDestination.getTimeSinceEpochMillis();

    int currentOffsetSeconds =
        currentZoneId
            .getRules()
            .getOffset(Instant.ofEpochMilli(destinationTimeUtcMillis))
            .getTotalSeconds();
    int destinationOffsetSeconds = timeAtDestination.getZoneOffsetSeconds();

    DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(context);

    if (currentOffsetSeconds == destinationOffsetSeconds) {
      // The destination is in the same time zone, so we don't need to display the time zone
      // string.
      dateFormat.setTimeZone(TimeZone.getTimeZone(currentZoneId));
      return dateFormat.format(destinationTimeUtcMillis);
    } else {
      // The destination is in a different timezone: calculate its zone offset and use it to
      // format
      // the time.
      TimeZone destinationZone;
      try {
        destinationZone = TimeZone.getTimeZone(ZoneOffset.ofTotalSeconds(destinationOffsetSeconds));
      } catch (DateTimeException e) {
        // This should never happen as the client library has checks to prevent this.
        L.e(LogTags.TEMPLATE, e, "Failed to get destination time zone, will use system default");
        destinationZone = TimeZone.getDefault();
      }

      dateFormat.setTimeZone(destinationZone);
      String timeAtDestinationString = dateFormat.format(destinationTimeUtcMillis);
      String zoneShortName = timeAtDestination.getZoneShortName();

      if (TextUtils.isEmpty(zoneShortName)) {
        // This should never really happen, the client library has checks to enforce a non
        // empty
        // zone name.
        L.w(LogTags.TEMPLATE, "Time zone name is empty when formatting date time");
        return timeAtDestinationString;
      } else {
        return context
            .getResources()
            .getString(
                context.getHostResourceIds().getTimeAtDestinationWithTimeZoneStringFormat(),
                timeAtDestinationString,
                zoneShortName);
      }
    }
  }

  private DateTimeUtils() {}
}
