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

import androidx.annotation.NonNull;
import androidx.car.app.model.Place;
import androidx.lifecycle.LifecycleRegistry;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents a layout that wraps a map view. */
public interface MapViewContainer {
  /**
   * Returns the {@link LifecycleRegistry} instance that can be used by a parent of the container to
   * drive the lifecycle events of the map view wrapped by it.
   */
  @NonNull
  LifecycleRegistry getLifecycleRegistry();

  /**
   * Sets whether current location is enabled.
   *
   * @param enable true if the map should show the current location
   */
  void setCurrentLocationEnabled(boolean enable);

  /** Sets the map anchor. The camera will be adjusted to include the anchor marker if necessary. */
  void setAnchor(@Nullable Place anchor);

  /**
   * Sets the places to display in the map. The camera will be moved to the region that contains all
   * the places.
   */
  void setPlaces(List<Place> places);
}
