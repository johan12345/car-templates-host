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
package com.android.car.libraries.apphost.nav;

import androidx.car.app.navigation.INavigationHost;
import androidx.car.app.navigation.model.Trip;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import com.android.car.libraries.apphost.AbstractHost;
import com.android.car.libraries.apphost.Host;
import com.android.car.libraries.apphost.common.CarAppError;
import com.android.car.libraries.apphost.common.StringUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.common.ThreadUtils;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryEvent;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link Host} implementation that handles communication between the client app and the rest of
 * the host.
 *
 * <p>Host services are per app, and live for the duration of a car session.
 */
public class NavigationHost extends AbstractHost {
  private final NavigationManagerDispatcher mDispatcher;
  private final NavigationHostStub mNavHostStub = new NavigationHostStub();

  @Nullable private Trip mTrip;
  private final NavigationStateCallback mNavigationStateCallback;

  /** Number of status events to store in a circular buffer for debug reports */
  private static final int MAX_STATUS_ITEMS = 10;

  /**
   * A circular buffer which will hold at most {@link #MAX_STATUS_ITEMS}. Items are added at the top
   * of the list and deleted from the end.
   */
  private final ArrayDeque<StatusItem> mStatusItemList = new ArrayDeque<>();

  /** Creates a {@link NavigationHost} instance. */
  public static NavigationHost create(
      Object appBinding, TemplateContext templateContext, NavigationStateCallback callback) {
    return new NavigationHost(
        NavigationManagerDispatcher.create(appBinding), templateContext, callback);
  }

  @Override
  public INavigationHost.Stub getBinder() {
    assertIsValid();
    return mNavHostStub;
  }

  /** Returns the {@link Trip} instance currently set in this host. */
  @Nullable
  public Trip getTrip() {
    assertIsValid();
    return mTrip;
  }

  @Override
  public void reportStatus(PrintWriter pw, Pii piiHandling) {
    if (mStatusItemList.isEmpty()) {
      pw.println("No navigation status events stored.");
      return;
    }
    long currentTime = System.currentTimeMillis();
    // TODO(b/177353816): Update after TableWriter is accessible in the host.
    pw.printf(
        "Event   | First Event Delta (millis), Num Consecutive Events, Last Event Delta"
            + " (millis)\n");
    for (StatusItem item : mStatusItemList) {
      pw.printf(
          "%8s| %s, %d, %s\n",
          item.mEventType.name(),
          StringUtils.formatDuration(currentTime - item.mInitialEventMillis),
          item.mNumConsecutiveEvents,
          StringUtils.formatDuration(currentTime - item.mFinalEventMillis));
    }
  }

  @Override
  public void onDisconnectedEvent() {
    mNavigationStateCallback.onNavigationEnded();
  }

  @Override
  public void onUnboundEvent() {
    mNavigationStateCallback.onNavigationEnded();
  }

  private void setTrip(@Nullable Trip trip) {
    mTrip = trip;
  }

  private NavigationHost(
      NavigationManagerDispatcher dispatcher,
      TemplateContext templateContext,
      NavigationStateCallback navigationStateCallback) {
    super(templateContext, LogTags.NAVIGATION);
    mNavigationStateCallback = navigationStateCallback;
    mDispatcher = dispatcher;
  }

  private final class NavigationHostStub extends INavigationHost.Stub {
    @Override
    public void updateTrip(Bundleable tripBundle) {
      runIfValid(
          "updateTrip",
          () -> {
            try {
              Trip trip = (Trip) tripBundle.get();
              ThreadUtils.runOnMain(
                  () -> {
                    addStatusItem(StatusItem.EventType.UPDATE);
                    if (mNavigationStateCallback.onUpdateTrip(trip)) {
                      setTrip(trip);
                    }
                  });
            } catch (BundlerException e) {
              mTemplateContext
                  .getErrorHandler()
                  .showError(CarAppError.builder(mDispatcher.getAppName()).setCause(e).build());
            }

            mTemplateContext
                .getTelemetryHandler()
                .logCarAppTelemetry(
                    TelemetryEvent.newBuilder(
                        UiAction.NAVIGATION_TRIP_UPDATED,
                        mTemplateContext.getCarAppPackageInfo().getComponentName()));
          });
    }

    @Override
    public void navigationStarted() {
      runIfValid(
          "navigationStarted",
          () -> {
            L.i(LogTags.NAVIGATION, "%s started navigation", getAppPackageName());
            ThreadUtils.runOnMain(
                () -> {
                  addStatusItem(StatusItem.EventType.START);
                  mNavigationStateCallback.onNavigationStarted(
                      () -> {
                        addStatusItem(StatusItem.EventType.STOP);
                        mDispatcher.dispatchStopNavigation(mTemplateContext);
                      });
                });

            mTemplateContext
                .getTelemetryHandler()
                .logCarAppTelemetry(
                    TelemetryEvent.newBuilder(
                        UiAction.NAVIGATION_STARTED,
                        mTemplateContext.getCarAppPackageInfo().getComponentName()));
          });
    }

    @Override
    public void navigationEnded() {
      if (!isValid()) {
        L.w(LogTags.NAVIGATION, "Accessed navigationEnded after host became invalidated");
      }
      // Run even if not valid so we cleanup state.

      L.i(LogTags.NAVIGATION, "%s ended navigation", getAppPackageName());
      ThreadUtils.runOnMain(
          () -> {
            addStatusItem(StatusItem.EventType.END);
            mNavigationStateCallback.onNavigationEnded();
          });
      mTemplateContext
          .getTelemetryHandler()
          .logCarAppTelemetry(
              TelemetryEvent.newBuilder(
                  UiAction.NAVIGATION_ENDED,
                  mTemplateContext.getCarAppPackageInfo().getComponentName()));
    }
  }

  private String getAppPackageName() {
    return mTemplateContext.getCarAppPackageInfo().getComponentName().getPackageName();
  }

  private void addStatusItem(StatusItem.EventType eventType) {
    StatusItem item = mStatusItemList.peekFirst();
    long timestampMillis = System.currentTimeMillis();

    if (item != null && item.mEventType == eventType) {
      item.appendTimeStamp(System.currentTimeMillis());
      return;
    }
    item = new StatusItem(eventType, timestampMillis);
    mStatusItemList.addFirst(item);
    while (mStatusItemList.size() > MAX_STATUS_ITEMS) {
      mStatusItemList.removeLast();
    }
  }

  /**
   * Entry for reporting the various events that can be reported on.
   *
   * <p>Only saves the time of the first and last event along with a count of the total events.
   */
  private static class StatusItem {
    enum EventType {
      START,
      UPDATE,
      END,
      STOP
    };

    final EventType mEventType;
    final long mInitialEventMillis;
    int mNumConsecutiveEvents;
    long mFinalEventMillis;

    StatusItem(EventType eventType, long initialEventMillis) {
      mEventType = eventType;
      mInitialEventMillis = initialEventMillis;
      mNumConsecutiveEvents = 1;
      mFinalEventMillis = initialEventMillis;
    }

    public void appendTimeStamp(long timestampMillis) {
      mNumConsecutiveEvents++;
      mFinalEventMillis = timestampMillis;
    }
  }
}
