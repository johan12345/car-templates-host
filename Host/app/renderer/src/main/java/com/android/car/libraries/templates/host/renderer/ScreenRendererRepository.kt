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
package com.android.car.libraries.templates.host.renderer

import android.content.ComponentName
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.logging.StatusReporter
import com.android.car.libraries.templates.host.internal.StatusManager
import com.google.common.collect.ImmutableList
import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

/** A cache to store instances of active [ScreenRenderer] that is safe for concurrent accesses. */
object ScreenRendererRepository : StatusReporter {

  // TODO(b/169643103): Change to Guava LRU cache to avoid having potential memory leaks.
  private val cache: ConcurrentHashMap<ComponentName, ScreenRenderer> = ConcurrentHashMap()

  init {
    StatusManager.addStatusReporter(StatusManager.ReportSection.SCREEN_RENDERES, this)
  }

  /**
   * Returns the value for the given [key]. If the key is not found in the cache, creates a
   * [ScreenRenderer] using the provided [screenRendererProvider], puts its result into the map
   * under the given key and returns it.
   *
   * This method guarantees not to put the value into the map if the key is already there, but the
   * [screenRendererProvider] may be invoked even if the key is already in the map.
   */
  fun computeIfAbsent(
    key: ComponentName,
    screenRendererProvider: Supplier<ScreenRenderer>
  ): ScreenRenderer {
    return cache.getOrPut(key) { screenRendererProvider.get() }
  }

  /** Returns the [ScreenRenderer] for the given [key] if available. */
  fun get(key: ComponentName): ScreenRenderer? {
    return cache[key]
  }

  /** Returns a copy of all the available [ScreenRenderer]s. */
  fun getAll(): ImmutableList<ScreenRenderer> {
    return     ImmutableList.copyOf(cache.values)
  }

  /** Removes the [ScreenRenderer] associated with the given [key] if available. */
  fun remove(key: ComponentName): ScreenRenderer? {
    return cache.remove(key)
  }

  /** Clears the cache content. */
  fun clear() {
    cache.clear()
  }

  override fun reportStatus(pw: PrintWriter, piiHandling: StatusReporter.Pii) {
    try {
      pw.println("ScreenRenderer cache")
      pw.printf("- size: %d\n", cache.size)
      pw.printf("- screenRenderers: %d\n", cache.size)
      for ((name, value) in cache.toSortedMap()) {
        pw.println("\n-------------------------------")
        pw.printf("App: %s\n", name.flattenToShortString())
        value.reportStatus(pw, piiHandling)
      }
    } catch (t: Throwable) {
      L.e(LogTags.APP_HOST, t, "Failed to produce status report for screen renderer cache")
    }
  }
}
