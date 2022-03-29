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

import android.content.ComponentName
import androidx.annotation.MainThread
import com.android.car.libraries.apphost.CarHost
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.logging.StatusReporter
import com.google.common.base.Supplier
import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap

/** Manages a cache of [CarHost]s. */
@MainThread
object CarHostRepository : StatusReporter {
  private val cache: MutableMap<ComponentName, CarHost> = ConcurrentHashMap()

  init {
    StatusManager.addStatusReporter(StatusManager.ReportSection.APP_HOST, this)
  }

  /**
   * Returns a [CarHost] for the given `appName` to use for app communication. If the key is not
   * present in the cache, uses the [Supplier] to retrieve the [CarHost] and puts it in the cache.
   */
  @Synchronized
  fun computeIfAbsent(appName: ComponentName, carHostSupplier: Supplier<CarHost>): CarHost {
    return cache.computeIfAbsent(appName) { carHostSupplier.get() }
  }

  /**
   * @return a [CarHost] for the given [appName] to use for app communication, or `null` if the key
   * is not present in the cache.
   */
  @Synchronized
  fun get(appName: ComponentName): CarHost? {
    return cache[appName]
  }

  /** Invalidates and removes the [CarHost] from cache for the given [appName]. */
  @Synchronized
  fun remove(appName: ComponentName) {
    val carHost = cache.remove(appName)
    carHost?.unbindFromApp()
    carHost?.invalidate()
  }

  /** Invalidates all the [CarHost] objects in the cache and empties the cache. */
  @Synchronized
  fun clear() {
    if (cache.isNotEmpty()) {
      for (carHost in cache.values) {
        carHost.unbindFromApp()
        carHost.invalidate()
      }
    }
    cache.clear()
  }

  override fun reportStatus(pw: PrintWriter, piiHandling: StatusReporter.Pii) {
    try {
      pw.println("Car host cache")
      pw.printf("- size: %d\n", cache.size)
      pw.printf("- hosts: %d\n", cache.size)
      for ((name, value) in cache) {
        pw.println("\n-------------------------------")
        pw.printf("Host: %s\n", name.flattenToShortString())
        value.reportStatus(pw, piiHandling)
      }
    } catch (t: Throwable) {
      L.e(LogTags.APP_HOST, t, "Failed to produce status report for car host cache")
    }
  }
}
