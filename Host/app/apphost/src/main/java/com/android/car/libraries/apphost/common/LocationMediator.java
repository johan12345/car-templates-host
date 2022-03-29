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

import android.location.Location;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.Place;
import java.util.List;

/**
 * A mediator for communicating {@link Place}s, and related information, from and to different
 * components in the UI hierarchy.
 */
public interface LocationMediator extends AppHostService {
  /**
   * Listener for notifying location changes by the app.
   *
   * <p>We do not go through the EventManager because we need to keep track of the listeners that
   * are registered so we know when to start and stop requesting location updates from the app.
   */
  interface AppLocationListener {
    void onAppLocationChanged(Location location);
  }

  /** Returns the current set of places of interest, or an empty list if there are none. */
  List<Place> getCurrentPlaces();

  /** Set a new list of places. */
  void setCurrentPlaces(List<Place> places);

  /** Returns the point when the camera was last anchored, or {@code null} if there was none. */
  @Nullable
  CarLocation getCameraAnchor();

  /** Set the center point of where the camera is anchored, or {@code null} if it is unknown. */
  void setCameraAnchor(@Nullable CarLocation cameraAnchor);

  /**
   * Add a listener for getting app location updates.
   *
   * <p>Note that using this on {@link androidx.car.app.versioning.CarAppApiLevel} 3 or lower would
   * have no effect.
   */
  void addAppLocationListener(AppLocationListener listener);

  /**
   * Removes the listener which stops it from receiving app location updates.
   *
   * <p>Note that using this on {@link androidx.car.app.versioning.CarAppApiLevel} 3 or lower would
   * have no effect.
   */
  void removeAppLocationListener(AppLocationListener listener);

  /**
   * Sets the {@link Location} as provided by the app.
   *
   * <p>This will notify the {@link AppLocationListener} that have been registered.
   */
  void setAppLocation(Location location);
}
