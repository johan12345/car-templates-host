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

import android.graphics.Insets
import android.os.Build
import android.view.WindowInsets
import androidx.car.app.activity.renderer.IInsetsListener
import androidx.car.app.utils.ThreadUtils
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.view.AbstractTemplateView

/** Handles window insets from the car app. */
class InsetsListener(private val templateView: AbstractTemplateView) : IInsetsListener.Stub() {
  override fun onInsetsChanged(insets: Insets) {
    ThreadUtils.runOnMain {
      L.i(LogTags.APP_HOST) { "Received insets: $insets" }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        templateView.windowInsets =
          WindowInsets.Builder()
            .setInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime(), insets)
            .build()
      } else {
        templateView.windowInsets = WindowInsets.Builder().setSystemWindowInsets(insets).build()
      }
    }
  }
}
