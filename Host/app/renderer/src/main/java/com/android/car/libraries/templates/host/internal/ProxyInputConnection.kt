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

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import androidx.car.app.activity.renderer.IProxyInputConnection
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Proxies an [InputConnection] across a binder interface. All InputConnection calls are made on the
 * main thread.
 *
 * Please note that once an InputConnection is invalid, it never becomes valid again. An invalid
 * InputConnection simply ignores calls that are made to it.
 *
 * Some InputConnection methods simply return a boolean indicating whether the input connection is
 * still valid. For these methods, we run the action and update the validity of the input connection
 * asynchronously - there's no need to synchronize this so long as the action happens on the main
 * thread. For all other methods where the return value matters, we block on the Binder thread until
 * the value has been provided on the main thread.
 */
class ProxyInputConnection(
  private val inputConnection: InputConnection,
  private val editorInfo: EditorInfo
) : IProxyInputConnection.Stub() {
  @Volatile private var inputConnectionValid = true
  private val handler = Handler(Looper.getMainLooper())

  override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
    return runOnMainAndAwaitResult(null) { inputConnection.getTextBeforeCursor(n, flags) }
  }

  override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
    return runOnMainAndAwaitResult(null) { inputConnection.getTextAfterCursor(n, flags) }
  }

  override fun getSelectedText(flags: Int): CharSequence? {
    return runOnMainAndAwaitResult(null) { inputConnection.getSelectedText(flags) }
  }

  override fun getCursorCapsMode(reqModes: Int): Int {
    return runOnMainAndAwaitResult(0) { inputConnection.getCursorCapsMode(reqModes) }
  }

  override fun beginBatchEdit(): Boolean {
    return runOnMainAndAwaitResult(false) { inputConnection.beginBatchEdit() }
  }

  override fun endBatchEdit(): Boolean {
    return runOnMainAndAwaitResult(false) { inputConnection.endBatchEdit() }
  }

  override fun sendKeyEvent(event: KeyEvent): Boolean {
    return runOnMainAndAwaitResult(false) { inputConnection.sendKeyEvent(event) }
  }

  override fun commitCorrection(correctionInfo: CorrectionInfo): Boolean {
    return runOnMainAndAwaitResult(false) { inputConnection.commitCorrection(correctionInfo) }
  }

  override fun commitCompletion(text: CompletionInfo?): Boolean {
    return runOnMainAndAwaitResult(false) { inputConnection.commitCompletion(text) }
  }

  override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
    return runOnMainAndAwaitResult(null) { inputConnection.getExtractedText(request, flags) }
  }

  override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
    return runOnMainAndUpdateValidity {
      inputConnection.deleteSurroundingText(beforeLength, afterLength)
    }
  }

  override fun setComposingText(text: CharSequence, newCursorPosition: Int): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.setComposingText(text, newCursorPosition) }
  }

  override fun setComposingRegion(start: Int, end: Int): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.setComposingRegion(start, end) }
  }

  override fun finishComposingText(): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.finishComposingText() }
  }

  override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.commitText(text, newCursorPosition) }
  }

  override fun setSelection(start: Int, end: Int): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.setSelection(start, end) }
  }

  override fun performEditorAction(editorAction: Int): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.performEditorAction(editorAction) }
  }

  override fun performContextMenuAction(id: Int): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.performContextMenuAction(id) }
  }

  override fun clearMetaKeyStates(states: Int): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.clearMetaKeyStates(states) }
  }

  override fun reportFullscreenMode(enabled: Boolean): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.reportFullscreenMode(enabled) }
  }

  override fun performPrivateCommand(action: String, data: Bundle): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.performPrivateCommand(action, data) }
  }

  override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
    return runOnMainAndUpdateValidity { inputConnection.requestCursorUpdates(cursorUpdateMode) }
  }

  override fun closeConnection() {
    runOnMainDirect {
      inputConnection.closeConnection()
      inputConnectionValid = false
    }
  }

  override fun getEditorInfo(): EditorInfo {
    return editorInfo
  }

  /**
   * Runs code on the main thread, and blocks for the result on another.
   *
   * @param defaultResult the value to return if event timeout or if the connection is invalid.
   * @param action the code to execute, that should return a result.
   * @return the value produced by [action].
   */
  private fun <T> runOnMainAndAwaitResult(defaultResult: T, action: Callable<T>): T {
    if (!inputConnectionValid) {
      return defaultResult
    }
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
      defaultResult
    } catch (e: TimeoutException) {
      defaultResult
    }
  }

  /**
   * Runs code on the main thread, and updates the [inputConnectionValid] with the result.
   *
   * @param action the code to execute, that should return a boolean indicating the validity of the
   * connection.
   * @return the value produced by [action]
   */
  private fun runOnMainAndUpdateValidity(action: Callable<Boolean>): Boolean {
    if (!inputConnectionValid) {
      return false
    }

    runOnMainDirect {
      try {
        inputConnectionValid = action.call()
      } catch (ex: Exception) {
        inputConnectionValid = false
        throw RuntimeException("Input connection action failed", ex)
      }
    }

    return true
  }

  /**
   * Runs code on the main thread. Does not jump thread if already on the main thread.
   *
   * @param action the code to execute.
   */
  private fun runOnMainDirect(action: Runnable) {
    if (Looper.myLooper() == handler.looper) {
      action.run()
    } else {
      handler.post(action)
    }
  }

  companion object {
    private const val ASYNC_TIMEOUT_MILLIS: Long = 1000
  }
}
