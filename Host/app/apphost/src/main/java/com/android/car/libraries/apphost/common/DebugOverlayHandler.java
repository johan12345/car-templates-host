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

import androidx.annotation.MainThread;
import androidx.car.app.model.TemplateWrapper;

/**
 * The interface for forwarding custom debug overlay information to the host fragment or activity.
 */
@MainThread
public interface DebugOverlayHandler {
  /**
   * Returns {@code true} if the debug overlay is active.
   *
   * <p>The caller can use the active state to determine whether to process debug overlay
   * information or not.
   */
  boolean isActive();

  /**
   * Sets debug overlay as active/inactive if parameter is {@code true}/{@code false} respectively.
   */
  void setActive(boolean active);

  /** Clears all existing debug overlay. */
  void clearAllEntries();

  /**
   * Removes the debug overlay entry associated with the input {@code debugKey}.
   *
   * <p>If the {@code debugKey} is not associated with any existing entry, this call is a no-op.
   */
  void removeDebugOverlayEntry(String debugKey);

  /**
   * Updates the debug overlay entry associated with a given {@code debugKey}.
   *
   * <p>This would override any previous debug text for the same key.
   */
  void updateDebugOverlayEntry(String debugKey, String debugOverlayText);

  /** Returns text to render for debug overlay. */
  CharSequence getDebugOverlayText();

  /** Resets debug overlay with new information from {@link TemplateWrapper} */
  void resetTemplateDebugOverlay(TemplateWrapper templateWrapper);

  /** Set {@link Observer} for this {@link DebugOverlayHandler} */
  void setObserver(Observer observer);

  /**
   * The interface that lets an object observe changes to the {@link DebugOverlayHandler}'s entries.
   */
  interface Observer {
    void entriesUpdated();
  }
}
