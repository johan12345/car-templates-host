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
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.car.app.activity.renderer.IProxyInputConnection
import com.android.car.libraries.apphost.input.CarEditable
import com.android.car.libraries.apphost.input.CarEditableListener
import com.android.car.libraries.apphost.input.InputManager
import com.android.car.libraries.apphost.logging.LogTags

/** The app specific implementation of [InputManager]. */
class InputManagerImpl(private val listener: InputManagerListener) : InputManager {

  /** A listener to be notified for input related events. */
  interface InputManagerListener {
    /* Should start the input, i.e. show soft keyboard */
    fun onStartInput()

    /* Should stop the input, i.e. hide soft keyboard */
    fun onStopInput()

    /*
     * Update the text selection. Gets called whenever text selection changes on the
     * [currentEditable].
     */
    fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int)
  }

  private var currentEditable: CarEditable? = null
  private val handler = Handler(Looper.getMainLooper())

  private var stopInputRunnable = Runnable {
    if (isInputActive) {
      currentEditable = null
      listener.onStopInput()
    }
  }

  private val carEditableListener =
      CarEditableListener { oldSelStart, oldSelEnd, newSelStart, newSelEnd ->
    listener.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd)
  }

  override fun startInput(view: CarEditable) {
    currentEditable?.setCarEditableListener(null)
    currentEditable = view
    currentEditable?.setCarEditableListener(carEditableListener)

    // Cancel any ongoing stop input to avoid jarring keyboard animations.
    handler.removeCallbacks(stopInputRunnable)

    listener.onStartInput()
  }

  override fun stopInput() {
    currentEditable?.setCarEditableListener(null)

    // Perform stop input with a delay to avoid jarring keyboard disappear+reappear animation
    // when switching form one focusable to another.
    handler.removeCallbacks(stopInputRunnable)
    handler.postDelayed(stopInputRunnable, STOP_INPUT_DELAY_MILLIS)
  }

  override fun isValid() = true
  override fun isInputActive() = currentEditable != null

  fun onCreateInputConnection(editorInfo: EditorInfo): IProxyInputConnection? {
    val currentEditable =
      currentEditable
        ?: run {
          Log.d(LogTags.APP_HOST, "There is no focusable target selected.")
          return null
        }
    val inputConnection =
      currentEditable.onCreateInputConnection(editorInfo)
        ?: run {
          Log.d(LogTags.APP_HOST, "Failed to create input connection for editorInfo $editorInfo")
          return null
        }
    return ProxyInputConnection(inputConnection, editorInfo)
  }

  companion object {
    const val STOP_INPUT_DELAY_MILLIS = 100L
  }
}
