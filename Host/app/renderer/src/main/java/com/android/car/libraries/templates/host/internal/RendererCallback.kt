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

import android.os.Handler
import android.os.Looper
import android.view.inputmethod.EditorInfo
import androidx.car.app.CarContext
import androidx.car.app.activity.renderer.IProxyInputConnection
import androidx.car.app.activity.renderer.IRendererCallback
import androidx.car.app.utils.ThreadUtils
import androidx.lifecycle.Lifecycle
import com.android.car.libraries.apphost.CarHost
import com.android.car.libraries.apphost.logging.L
import com.android.car.libraries.apphost.logging.LogTags
import com.android.car.libraries.apphost.template.AppHost
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** Handles events from the car app. */
class RendererCallback(private val carHost: CarHost, private val inputManager: InputManagerImpl) :
  IRendererCallback.Stub() {
  private val handler = Handler(Looper.getMainLooper())

  override fun onBackPressed() {
    val appHost = carHost.getHostOrThrow(CarContext.APP_SERVICE) as? AppHost
    appHost?.onBackPressed()
  }

  override fun onCreate() {
    ThreadUtils.runOnMain { carHost.dispatchAppLifecycleEvent(Lifecycle.Event.ON_CREATE) }
  }

  override fun onStart() {
    ThreadUtils.runOnMain { carHost.dispatchAppLifecycleEvent(Lifecycle.Event.ON_START) }
  }

  override fun onResume() {
    ThreadUtils.runOnMain { carHost.dispatchAppLifecycleEvent(Lifecycle.Event.ON_RESUME) }
  }

  override fun onPause() {
    ThreadUtils.runOnMain {
      try {
        carHost.dispatchAppLifecycleEvent(Lifecycle.Event.ON_PAUSE)
      } catch (e: IllegalStateException) {
        // Don't crash when dispatching on shutdown as you can run into race conditions.
      }
    }
  }

  override fun onStop() {
    ThreadUtils.runOnMain {
      try {
        carHost.dispatchAppLifecycleEvent(Lifecycle.Event.ON_STOP)
      } catch (e: IllegalStateException) {
        // Don't crash when dispatching on shutdown as you can run into race conditions.
      }
    }
  }

  override fun onDestroyed() {
    // Unlike the other lifecycle events, the fact that the CarAppActivity is destroyed does
    // not mean that the CarAppBinding should be destroyed or unbound. We already have logic
    // in CarHost to unbind the CarAppService after a specific timeout if the app remains in the
    // STOPPED state (for non-nav apps).
  }

  override fun onCreateInputConnection(editorInfo: EditorInfo): IProxyInputConnection? {
    return runOnMainAndAwaitResult { inputManager.onCreateInputConnection(editorInfo) }
  }

  /**
   * Runs code on the main thread, and waits for the result.
   *
   * @param action the code to execute, that should return a result.
   * @return the value produced by [action]. Returns null if times out or interrupted.
   */
  private fun <T> runOnMainAndAwaitResult(action: Callable<T>): T? {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      return try {
        action.call()
      } catch (e: Exception) {
        throw RuntimeException(e)
      }
    }
    val futureTask = FutureTask(action)
    handler.post(futureTask)
    return try {
      futureTask[ASYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS]
    } catch (e: ExecutionException) {
      throw RuntimeException(e)
    } catch (e: InterruptedException) {
      L.e(LogTags.APP_HOST, e, "Running call on main was interrupted.")
      null
    } catch (e: TimeoutException) {
      L.e(LogTags.APP_HOST, e, "Running call on main was timed out.")
      null
    }
  }

  companion object {
    private const val ASYNC_TIMEOUT_MILLIS: Long = 1000
  }
}
