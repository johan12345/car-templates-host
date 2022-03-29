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

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/** Assorted string manipulation utilities. */
public class StringUtils {
  /** Milliseconds per unit of time LUT. Needs to be in sync with {@link #UNIT_SUFFIXES}. */
  private static final long[] MILLIS_PER_UNIT =
      new long[] {
        DAYS.toMillis(1),
        HOURS.toMillis(1),
        MINUTES.toMillis(1),
        SECONDS.toMillis(1),
        1 // 1 millisecond in milliseconds
      };

  private static final String[] UNIT_SUFFIXES = new String[] {"d", "h", "m", "s", "ms"};

  /**
   * Returns a compact string representation of a duration.
   *
   * <p>The format is {@code "xd:xh:xm:xs:xms"}, where {@code "x"} is an unpadded numeric value. If
   * {@code "x"} is 0, it is altogether omitted.
   *
   * <p>For example, {@code "1d:25m:123ms"} denotes 1 day, 25 minutes, and 123 milliseconds.
   *
   * <p>Negative durations are returned as {@code "-"}
   */
  public static String formatDuration(long durationMillis) {
    StringBuilder builder = new StringBuilder();
    if (durationMillis < 0) {
      return "-";
    } else if (durationMillis == 0) {
      return "0ms";
    }
    boolean first = true;
    for (int i = 0; i < MILLIS_PER_UNIT.length; ++i) {
      long value =
          (i > 0 ? (durationMillis % MILLIS_PER_UNIT[i - 1]) : durationMillis) / MILLIS_PER_UNIT[i];
      if (value > 0) {
        if (first) {
          first = false;
        } else {
          builder.append(":");
        }
        builder.append(value).append(UNIT_SUFFIXES[i]);
      }
    }
    return builder.toString();
  }

  private StringUtils() {}
}
