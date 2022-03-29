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
package com.android.car.libraries.templates.host.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build

/** Holds static util methods for common usage in the host. */
object CommonUtils {
  /**
   * Key for the extra that we insert into an Intent to mark it as coming from a notification
   * action.
   */
  const val EXTRA_NOTIFICATION_INTENT = "CAR_APP_NOTIFICATION_INTENT"

  /** Checks whether the templates host is currently running on an emulator. */
  private fun isConnectedToEmulator(): Boolean {
    return Build.PRODUCT.contains("gcar") ||
      Build.FINGERPRINT.contains("unknown") ||
      Build.FINGERPRINT.contains("emu") ||
      Build.DEVICE.contains("generic") ||
      Build.DEVICE.contains("emu")
  }

  /** Checks whether the templates host has debug mode enabled */
  fun isDebugEnabled(context: Context): Boolean {
    return isConnectedToEmulator() ||
      (0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
  }
}
