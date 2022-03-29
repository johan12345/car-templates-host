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

package com.android.car.libraries.apphost.common;

import android.os.Handler;
import android.os.Looper;

/** Utility functions to handle running functions on the main thread. */
public class ThreadUtils {
  private static final Handler HANDLER = new Handler(Looper.getMainLooper());

  /** Field assignment is atomic in java and we are only checking reference equality. */
  private static Thread sMainThread;

  /** Executes the {@code action} on the main thread. */
  public static void runOnMain(Runnable action) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
      action.run();
    } else {
      HANDLER.post(action);
    }
  }

  /** Enqueues the {@code action} to the message queue on the main thread. */
  public static void enqueueOnMain(Runnable action) {
    HANDLER.post(action);
  }

  /**
   * Checks that currently running on the main thread.
   *
   * @throws IllegalStateException if the current thread is not the main thread
   */
  public static void checkMainThread() {
    if (Looper.getMainLooper() != Looper.myLooper()) {
      throw new IllegalStateException("Not running on main thread when it is required to.");
    }
  }

  /** Returns true if the current thread is the UI thread. */
  public static boolean getsMainThread() {
    if (sMainThread == null) {
      sMainThread = Looper.getMainLooper().getThread();
    }
    return Thread.currentThread() == sMainThread;
  }

  /** Checks that the current thread is the UI thread. Otherwise throws an exception. */
  public static void ensureMainThread() {
    if (!getsMainThread()) {
      throw new AssertionError("Must be called on the UI thread");
    }
  }

  private ThreadUtils() {}
}
