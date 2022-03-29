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

import android.content.Intent;

/** Controls the ability to start a new car app, as well as finish the current car app. */
public interface CarAppManager {
  /**
   * Starts a car app on the car screen.
   *
   * @see androidx.car.app.CarContext#startCarApp
   */
  void startCarApp(Intent intent);

  /** Unbinds from the car app, and goes to the app launcher if the app is currently foreground. */
  void finishCarApp();
}
