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
package com.android.car.libraries.apphost.input;

/**
 * Manages use of the in-car IME. All methods should only be called on the main thread.
 * TODO(b/174880910): Use @MainThread here.
 */
public interface InputManager {
  /**
   * Starts input on the requested {@link CarEditable}, showing the IME. If IME input is already
   * occurring for another view, this call stops input on the previous view and starts input on the
   * new view.
   *
   * <p>This method must only be called from the UI thread. This method should not be called from a
   * stopped activity.
   */
  void startInput(CarEditable view);

  /**
   * Stops input, hiding the IME. This method fails silently if the calling application didn't
   * request input and isn't the active IME.
   *
   * <p>This function must only be called from the UI thread.
   */
  void stopInput();

  /**
   * Returns {@code true} while the {@link InputManager} is valid. The {@link InputManager} is valid
   * as long as the activity from which it was obtained has been created and not destroyed.
   */
  boolean isValid();

  /**
   * Returns whether this {@link InputManager} is valid and the IME is active on the given {@link
   * CarEditable}.
   */
  boolean isInputActive();
}
