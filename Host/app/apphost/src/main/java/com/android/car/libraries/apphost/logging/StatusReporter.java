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

import java.io.PrintWriter;

/** An interface for a component that can contribute status to a bug report. */
public interface StatusReporter {
  /** Specifies how to handle PII in a status report. */
  enum Pii {
    /** Omit PII from the bug report. */
    HIDE,
    /** Show PII in the bug report. */
    SHOW
  }

  /**
   * Writes the status of this component to a bug report.
   *
   * @param pw A {@link PrintWriter} to which to write the status.
   * @param piiHandling How to handle PII in the report.
   */
  void reportStatus(PrintWriter pw, Pii piiHandling);
}
