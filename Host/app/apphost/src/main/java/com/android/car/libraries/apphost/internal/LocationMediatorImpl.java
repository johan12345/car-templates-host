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
package com.android.car.libraries.apphost.internal;

import android.location.Location;
import androidx.annotation.Nullable;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.Place;
import com.android.car.libraries.apphost.common.EventManager;
import com.android.car.libraries.apphost.common.EventManager.EventType;
import com.android.car.libraries.apphost.common.LocationMediator;
import com.android.car.libraries.apphost.common.ThreadUtils;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link LocationMediator}.
 *
 * <p>There's only one set of places available at any given time, so the last writer wins. This
 * class is not meant to be used with multiple publishers (e.g. shared between multiple apps) so
 * that is just fine.
 *
 * <p>This class is not safe for concurrent access.
 */
public class LocationMediatorImpl implements LocationMediator {
  /** Interface for requesting start and stop of location updates from an app. */
  public interface AppLocationUpdateRequester {
    /** Sets whether to get location updates from an app. */
    void enableLocationUpdates(boolean enabled);
  }

  @Nullable private CarLocation mCameraAnchor;
  private List<Place> mCurrentPlaces = ImmutableList.of();
  private final List<AppLocationListener> mAppLocationListeners = new ArrayList<>();
  private final EventManager mEventManager;
  private final AppLocationUpdateRequester mLocationUpdateRequester;

  /** Returns an instance of a {@link LocationMediator}. */
  public static LocationMediator create(
      EventManager eventManager, AppLocationUpdateRequester locationUpdateRequester) {
    return new LocationMediatorImpl(eventManager, locationUpdateRequester);
  }

  @Override
  public List<Place> getCurrentPlaces() {
    return mCurrentPlaces;
  }

  @Override
  public void setCurrentPlaces(List<Place> places) {
    ThreadUtils.ensureMainThread();

    if (mCurrentPlaces.equals(places)) {
      return;
    }
    mCurrentPlaces = places;
    mEventManager.dispatchEvent(EventType.PLACE_LIST);
  }

  @Override
  @Nullable
  public CarLocation getCameraAnchor() {
    return mCameraAnchor;
  }

  @Override
  public void setCameraAnchor(@Nullable CarLocation cameraAnchor) {
    mCameraAnchor = cameraAnchor;
  }

  @Override
  public void addAppLocationListener(AppLocationListener listener) {
    if (mAppLocationListeners.isEmpty()) {
      mLocationUpdateRequester.enableLocationUpdates(true);
    }
    mAppLocationListeners.add(listener);
  }

  @Override
  public void removeAppLocationListener(AppLocationListener listener) {
    mAppLocationListeners.remove(listener);
    if (mAppLocationListeners.isEmpty()) {
      mLocationUpdateRequester.enableLocationUpdates(false);
    }
  }

  @Override
  public void setAppLocation(Location location) {
    for (AppLocationListener listener : mAppLocationListeners) {
      listener.onAppLocationChanged(location);
    }
  }

  private LocationMediatorImpl(
      EventManager eventManager, AppLocationUpdateRequester locationUpdateRequester) {
    mEventManager = eventManager;
    mLocationUpdateRequester = locationUpdateRequester;
  }
}
