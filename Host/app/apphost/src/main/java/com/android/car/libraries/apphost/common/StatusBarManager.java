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

import android.view.View;

/**
 * A manager that allows presenters to control some attributes of the status bar, such as the color
 * of the text and background.
 */
public interface StatusBarManager {
  /** The type of status bar to display. */
  enum StatusBarState {
    /**
     * The status bar is designed to be rendered over an app drawn surface such as a map, where it
     * will have a background protection to ensure the user can read the status bar information.
     */
    OVER_SURFACE,

    /**
     * The status bar is designed to be rendered over a dark background (e.g. white text with
     * transparent background).
     */
    LIGHT,

    /** The status bar is designed the status bar */
    GONE
  }

  /** Updates the {@link StatusBarState}. */
  void setStatusBarState(StatusBarState statusBarState, View rootView);
}
