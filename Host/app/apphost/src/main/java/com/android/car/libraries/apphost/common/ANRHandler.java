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

import com.android.car.libraries.apphost.logging.CarAppApi;

/** Handles checking if an app does not respond in a timely manner. */
public interface ANRHandler {
  /** Time to wait for ANR check. */
  int ANR_TIMEOUT_MS = 5000;

  /**
   * Performs the call and checks for application not responding.
   *
   * <p>The ANR check will happen in {@link #ANR_TIMEOUT_MS} milliseconds after calling {@link
   * ANRCheckingCall#call}.
   */
  void callWithANRCheck(CarAppApi carAppApi, ANRCheckingCall call);

  /** Token for dismissing the ANR check. */
  interface ANRToken {
    /** Requests dismissal of the ANR check. */
    void dismiss();

    /** Returns the {@link CarAppApi} that this token is for. */
    CarAppApi getCarAppApi();
  }

  /** A call that checks for ANR and receives a token to use for dismissing the ANR check. */
  interface ANRCheckingCall {
    /**
     * Performs the call.
     *
     * @param anrToken the token to use for dismissing the ANR check when the app calls back
     */
    void call(ANRToken anrToken);
  }
}
