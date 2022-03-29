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

import androidx.annotation.VisibleForTesting
import com.android.car.libraries.apphost.logging.StatusReporter
import java.io.PrintWriter
import java.util.SortedMap
import java.util.TreeMap

/** Manager to handle collecting status information from components to be added to a bug report. */
object StatusManager : StatusReporter {
  /** Sections to include in the status information */
  enum class ReportSection {
    APP_HOST,
    SCREEN_RENDERES,
  }

  private val statusReporters: SortedMap<ReportSection, StatusReporter> = TreeMap()
  private val lock = Any()

  /**
   * Adds a [StatusReporter] to be called for a bug report.
   *
   * @param section The section to be added to the bug report.
   * @param reporter The [StatusReporter] that will fill in the information for the section.
   */
  fun addStatusReporter(section: ReportSection, reporter: StatusReporter) {
    synchronized(lock) { statusReporters.put(section, reporter) }
  }

  /**
   * Removes the [StatusReporter] for a given bug report section.
   *
   * @param section The section to remove, as passed to [.addStatusReporter].
   */
  fun removeStatusReporter(section: ReportSection) {
    synchronized(lock) { statusReporters.remove(section) }
  }

  @VisibleForTesting
  fun clear() {
    synchronized(lock) { statusReporters.clear() }
  }

  override fun reportStatus(writer: PrintWriter, piiHandling: StatusReporter.Pii) {
    synchronized(lock) {
      for ((key, value) in statusReporters) {
        writer.format("=== %s ===\n", key.name)
        try {
          value.reportStatus(writer, piiHandling)
        } catch (throwable: Throwable) {
          writer.format("\nError capturing dump for section: %s\n", throwable.message)
          throwable.printStackTrace(writer)
        }
        writer.println()
      }
    }
  }
}
