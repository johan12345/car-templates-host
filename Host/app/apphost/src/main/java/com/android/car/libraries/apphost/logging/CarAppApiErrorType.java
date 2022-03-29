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
package com.android.car.libraries.apphost.logging;

/** Different errors that may happen due to a Car App Library IPC. */
// LINT.IfChange
public enum CarAppApiErrorType {
  UNKNOWN_ERROR,
  BUNDLER_EXCEPTION,
  ILLEGAL_STATE_EXCEPTION,
  INVALID_PARAMETER_EXCEPTION,
  SECURITY_EXCEPTION,
  RUNTIME_EXCEPTION,
  REMOTE_EXCEPTION,
  ANR
}
// LINT.ThenChange(//depot/google3/java/com/google/android/apps/auto/components/apphost/internal/\
//      TelemetryHandlerImpl.java,
//      //depot/google3/java/com/google/android/apps/automotive/templates/host/di/logging/\
//      ClearcutTelemetryLogger.java,
//      //depot/google3/logs/proto/wireless/android/automotive/templates/host/\
//      android_automotive_templates_host_info.proto)
