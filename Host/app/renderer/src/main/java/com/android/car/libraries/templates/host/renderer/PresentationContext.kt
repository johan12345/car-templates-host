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

import android.content.Context
import android.content.ContextWrapper
import android.view.Display
import android.view.inputmethod.InputMethodManager

/**
 * The context used for the [Presentation] of [LegacySurfaceController].
 *
 * This context injects its main [InputMethodManager] to its display contexts to avoid display
 * mismatch which results in polluted logs.
 */
internal class PresentationContext(base: Context) : ContextWrapper(base) {

  private class PresentationDisplayContext(
    base: Context,
    private val inputMethodManager: InputMethodManager
  ) : ContextWrapper(base) {

    override fun getSystemService(name: String): Any {
      return if (INPUT_METHOD_SERVICE == name) inputMethodManager else super.getSystemService(name)
    }
  }

  override fun createDisplayContext(display: Display): Context {
    val inputMethodManager =
      baseContext.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    val context = super.createDisplayContext(display)
    return PresentationDisplayContext(context, inputMethodManager)
  }
}
