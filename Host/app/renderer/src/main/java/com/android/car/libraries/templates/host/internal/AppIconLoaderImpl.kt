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
import android.content.Context
import android.graphics.drawable.Drawable
import com.android.car.libraries.apphost.common.AppIconLoader

/** Android Automotive implementation of [AppIconLoader] */
object AppIconLoaderImpl : AppIconLoader {
  override fun getRoundAppIcon(context: Context, componentName: ComponentName): Drawable {
    return try {
      val pm = context.packageManager
      val applicationInfo = pm.getApplicationInfo(componentName.packageName, 0)
      val appIconResId = applicationInfo.icon

      pm.getResourcesForApplication(componentName.packageName).getDrawable(appIconResId, null)
    } catch (ex: Exception) {
      getDefaultAppIcon(context)
    }
  }

  private fun getDefaultAppIcon(context: Context): Drawable {
    return context.resources.getDrawable(android.R.drawable.sym_def_app_icon, null)
  }
}
