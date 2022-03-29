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

import android.widget.Toast;

/** Allows controlling the toasts on car screen. */
public interface ToastController {
  /**
   * Shows the Toast view with the specified text for the specified duration.
   *
   * @param text the text message to be displayed
   * @param duration how long to display the message. Either {@link Toast#LENGTH_SHORT} or {@link
   *     Toast#LENGTH_LONG}
   */
  void showToast(CharSequence text, int duration);
}
