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

import android.os.SystemClock;

/**
 * Wrapper of SystemClock
 *
 * <p>Real instances should just delegate the calls to the static methods, while test instances
 * return values set manually. See {@link android.os.SystemClock}.
 */
public final class SystemClockWrapper {
  /**
   * Returns milliseconds since boot, including time spent in sleep.
   *
   * @return elapsed milliseconds since boot
   */
  public long elapsedRealtime() {
    return SystemClock.elapsedRealtime();
  }
}
