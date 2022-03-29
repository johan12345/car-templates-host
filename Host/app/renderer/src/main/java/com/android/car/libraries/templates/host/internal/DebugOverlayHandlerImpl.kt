/*
 * Copyright (C) 2022 Google Inc.
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

import androidx.car.app.model.TemplateWrapper
import com.android.car.libraries.apphost.common.DebugOverlayHandler
import java.util.LinkedHashMap

/** The handler for the template-specific debug overlay. */
class DebugOverlayHandlerImpl(private var isDebugOverlayActive: Boolean) : DebugOverlayHandler {
  /** Using a linked hashmap here to keep track of debug entries' orders */
  private val debugTextMap: HashMap<String, String> = LinkedHashMap()

  private val builder = StringBuilder()

  private var observer: DebugOverlayHandler.Observer? = null

  override fun setActive(active: Boolean) {
    isDebugOverlayActive = active
    observer?.entriesUpdated()
  }

  override fun isActive(): Boolean {
    return isDebugOverlayActive
  }

  override fun clearAllEntries() {
    debugTextMap.clear()
  }

  override fun removeDebugOverlayEntry(debugKey: String) {
    debugTextMap.remove(debugKey)
  }

  override fun updateDebugOverlayEntry(debugKey: String, debugOverlayText: String) {
    debugTextMap[debugKey] = debugOverlayText
  }

  override fun getDebugOverlayText(): CharSequence {
    builder.setLength(0)
    var needsNewLineBefore = false
    for (key in debugTextMap.keys) {
      if (needsNewLineBefore) {
        builder.append("\n")
      }
      builder.append(key).append(": ").append(debugTextMap[key])
      needsNewLineBefore = true
    }
    return builder.toString()
  }

  override fun setObserver(observer: DebugOverlayHandler.Observer?) {
    this.observer = observer
    observer?.entriesUpdated()
  }

  override fun resetTemplateDebugOverlay(templateWrapper: TemplateWrapper) {
    clearAllEntries()
    updateDebugOverlayEntry(
      /* debugKey= */ "Step",
      Integer.toString(templateWrapper.currentTaskStep)
    )
    updateDebugOverlayEntry(
      /* debugKey= */ "Template",
      templateWrapper.template.javaClass.simpleName
    )
    observer?.entriesUpdated()
  }
}
