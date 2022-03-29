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
package com.android.car.libraries.templates.host

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags

/** A [BroadcastReceiver] used to pre-warm the host and set it as a foreground service. */
class BootCompleteReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    L.d(LogTags.SERVICE) { "StartUpBootReceiver: received ${intent.action}" }
    val serviceIntent = Intent(context, RendererService::class.java)
    context.startForegroundService(serviceIntent)
  }
}
