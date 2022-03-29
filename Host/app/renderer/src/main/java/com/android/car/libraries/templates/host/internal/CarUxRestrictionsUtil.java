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
package com.android.car.libraries.templates.host.internal;

import android.car.Car;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictions.CarUxRestrictionsInfo;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.android.car.libraries.apphost.common.ThreadUtils;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Utility class to access Car Restriction Manager.
 *
 * <p>This class must be a singleton because only one listener can be registered with {@link
 * CarUxRestrictionsManager} at a time, as documented in {@link
 * CarUxRestrictionsManager#registerListener}.
 */
// TODO(b/187312393): Rename to CarUxRestrictionsManager
public class CarUxRestrictionsUtil {
  private static final String TAG = "CarUxRestrictionsUtil";

  @NonNull private CarUxRestrictions mCarUxRestrictions = getDefaultRestrictions();

  private final Set<OnUxRestrictionsChangedListener> mObservers =
      Collections.newSetFromMap(new WeakHashMap<>());

  private static CarUxRestrictionsUtil sInstance = null;

  private CarUxRestrictionsUtil(Context context) {
    CarUxRestrictionsManager.OnUxRestrictionsChangedListener listener =
        (carUxRestrictions) -> {
          if (carUxRestrictions == null) {
            mCarUxRestrictions = getDefaultRestrictions();
          } else {
            mCarUxRestrictions = carUxRestrictions;
          }

          ThreadUtils.runOnMain(
              () -> {
                for (OnUxRestrictionsChangedListener observer : mObservers) {
                  observer.onRestrictionsChanged(mCarUxRestrictions);
                }
              });
        };

    try {
      Car carApi = Car.createCar(context.getApplicationContext());

      try {
        CarUxRestrictionsManager carUxRestrictionsManager =
            (CarUxRestrictionsManager) carApi.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE);
        carUxRestrictionsManager.registerListener(listener);
        listener.onUxRestrictionsChanged(carUxRestrictionsManager.getCurrentCarUxRestrictions());
      } catch (NullPointerException e) {
        Log.e(TAG, "Car not connected", e);
        // mCarUxRestrictions will be the default
      }
    } catch (SecurityException e) {
      Log.w(TAG, "Unable to connect to car service, assuming unrestricted", e);
      listener.onUxRestrictionsChanged(
          new CarUxRestrictions.Builder(false, CarUxRestrictions.UX_RESTRICTIONS_BASELINE, 0)
              .build());
    }
  }

  @NonNull
  private static CarUxRestrictions getDefaultRestrictions() {
    return new CarUxRestrictions.Builder(
            true, CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED, 0)
        .build();
  }

  /** Listener interface used to update clients on UxRestrictions changes */
  public interface OnUxRestrictionsChangedListener {
    /** Called when CarUxRestrictions changes */
    void onRestrictionsChanged(@NonNull CarUxRestrictions carUxRestrictions);
  }

  /** Returns the singleton sInstance of this class */
  @NonNull
  public static CarUxRestrictionsUtil getInstance(Context context) {
    if (sInstance == null) {
      sInstance = new CarUxRestrictionsUtil(context);
    }

    return sInstance;
  }

  /**
   * Registers a listener on this class for updates to CarUxRestrictions. Multiple listeners may be
   * registered. Note that this class will only hold a weak reference to the listener, you must
   * maintain a strong reference to it elsewhere.
   */
  public void register(OnUxRestrictionsChangedListener listener) {

    ThreadUtils.runOnMain(
        () -> {
          mObservers.add(listener);
          listener.onRestrictionsChanged(mCarUxRestrictions);
        });
  }

  /** Unregisters a registered listener */
  public void unregister(OnUxRestrictionsChangedListener listener) {
    ThreadUtils.runOnMain(() -> mObservers.remove(listener));
  }

  @NonNull
  public CarUxRestrictions getCurrentRestrictions() {
    return mCarUxRestrictions;
  }

  /**
   * Returns whether any of the given flags are blocked by the specified restrictions. If null is
   * given, the method returns true for safety.
   */
  public static boolean isRestricted(
      @CarUxRestrictionsInfo int restrictionFlags, @Nullable CarUxRestrictions uxr) {
    return (uxr == null) || ((uxr.getActiveRestrictions() & restrictionFlags) != 0);
  }

  /** Sets car UX restrictions. Only used for testing. */
  @VisibleForTesting
  public void setUxRestrictions(CarUxRestrictions carUxRestrictions) {
    mCarUxRestrictions = carUxRestrictions;
  }
}
