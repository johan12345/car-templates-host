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
package com.android.car.libraries.apphost;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.CarContext;
import androidx.car.app.ICarApp;
import androidx.car.app.ICarHost;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import com.android.car.libraries.apphost.common.ANRHandler.ANRToken;
import com.android.car.libraries.apphost.common.EventManager.EventType;
import com.android.car.libraries.apphost.common.IntentUtils;
import com.android.car.libraries.apphost.common.NamedAppServiceCall;
import com.android.car.libraries.apphost.common.OnDoneCallbackStub;
import com.android.car.libraries.apphost.common.StringUtils;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.internal.CarAppBinding;
import com.android.car.libraries.apphost.internal.CarAppBindingCallback;
import com.android.car.libraries.apphost.logging.CarAppApi;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.StatusReporter;
import com.android.car.libraries.apphost.logging.TelemetryEvent;
import com.android.car.libraries.apphost.logging.TelemetryEvent.UiAction;
import com.android.car.libraries.apphost.logging.TelemetryHandler;
import com.google.common.base.Preconditions;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/** Host responsible for binding to and maintaining the lifecycle of a single app. */
public class CarHost implements LifecycleOwner, StatusReporter {
  // Suppress under-initialization checker warning for passing this to the LifecycleRegistry's
  // ctor.
  @SuppressWarnings("nullness")
  private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

  private final ICarHost.Stub mCarHostStub = new CarHostStubImpl();
  private final CarAppBinding mCarAppBinding;
  private final TelemetryHandler mTelemetryHandler;

  // Key is a @CarAppService.
  private final HashMap<String, Host> mHostServices = new HashMap<>();

  private TemplateContext mTemplateContext;
  private long mLastStartTimeMillis = -1;
  private boolean mIsValid = true;
  private boolean mIsAppBound = false;

  /** Creates a {@link CarHost}. */
  public static CarHost create(TemplateContext templateContext) {
    return new CarHost(templateContext);
  }

  /**
   * Binds to the app managed by this {@link CarHost} instance.
   *
   * @param intent the intent used to start the app.
   */
  public void bindToApp(Intent intent) {
    assertIsValid();

    for (Host host : mHostServices.values()) {
      host.onBindToApp(intent);
    }

    // Remove the custom extras we put in the intent, if any.
    IntentUtils.removeInternalIntentExtras(
        intent, mTemplateContext.getCarHostConfig().getHostIntentExtrasToRemove());

    mCarAppBinding.bind(intent);
    mTelemetryHandler.logCarAppTelemetry(
        TelemetryEvent.newBuilder(UiAction.APP_START, mCarAppBinding.getAppName()));
  }

  /** Unbinds from the app previously bound to with {@link #bindToApp}. */
  public void unbindFromApp() {
    mCarAppBinding.unbind();
  }

  /**
   * Registers a {@link Host} with this host and returns the {@link CarHost}. This call is
   * idempotent for the same {@code type}.
   *
   * @param type one of the CarServiceType as defined in {@link CarContext}
   * @param factory factory for creating the {@link Host} corresponding to the service type
   */
  public Host registerHostService(String type, HostFactory factory) {
    assertIsValid();
    Host host = mHostServices.get(type);
    if (host == null) {
      host = factory.createHost(mCarAppBinding);
      mHostServices.put(type, host);
    }
    return host;
  }

  /** Updates the {@link TemplateContext} when the template has destroyed an recreated. */
  public void setTemplateContext(TemplateContext templateContext) {
    // Since we are updating the TemplateContext, unsubscribe the event listener from the
    // previous one.
    mTemplateContext.getEventManager().unsubscribeEvent(this, EventType.CONFIGURATION_CHANGED);

    mTemplateContext = templateContext;

    mTemplateContext.getAppBindingStateProvider().updateAppBindingState(mIsAppBound);
    mTemplateContext
        .getEventManager()
        .subscribeEvent(this, EventType.CONFIGURATION_CHANGED, this::onConfigurationChanged);
    mCarAppBinding.setTemplateContext(templateContext);

    for (Host host : mHostServices.values()) {
      host.setTemplateContext(templateContext);
    }
  }

  @Override
  public String toString() {
    return mCarAppBinding.toString();
  }

  /**
   * Returns the {@link Host} that is registered for the given {@code type}.
   *
   * @param type one of the CarServiceType as defined in {@link CarContext}
   * @throws IllegalStateException if there are no services registered for the given {@code type}
   */
  public Host getHostOrThrow(String type) {
    assertIsValid();
    Host host = mHostServices.get(type);
    if (host == null) {
      throw new IllegalStateException("No host service registered for: " + type);
    }
    return host;
  }

  /** Dispatches the given lifecycle event to the app managed by this {host}. */
  public void dispatchAppLifecycleEvent(Event event) {
    Log.d(LogTags.APP_HOST, "AppLifecycleEvent: " + event);
    assertIsValid();
    mLifecycleRegistry.handleLifecycleEvent(event);
  }

  /** Invalidates the {@link CarHost} so that any subsequent call on any of the APIs will fail. */
  public void invalidate() {
    mIsValid = false;
    for (Host host : mHostServices.values()) {
      host.invalidateHost();
    }

    mLifecycleRegistry.handleLifecycleEvent(Event.ON_DESTROY);
  }

  /** Returns the {@link CarAppBinding} instance used to bind to the app. */
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public CarAppBinding getCarAppBinding() {
    return mCarAppBinding;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public TemplateContext getTemplateContext() {
    return mTemplateContext;
  }

  /** Runs the logic necessary after the {@link CarHost} has successfully bound to the app. */
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public void onAppBound() {
    // Don't assert whether the object is valid here as this is an asynchronous API and could be
    // called after being invalidated we don't want to cause a crash after the previous
    // shutdown.
    if (!mIsValid) {
      return;
    }

    mIsAppBound = true;
    mTemplateContext.getAppBindingStateProvider().updateAppBindingState(mIsAppBound);
    for (Host host : mHostServices.values()) {
      host.onCarAppBound();
    }

    // Binding is asynchronous, so when it completes, the lifecycle events may not have
    // propagated. When lifecycle events happen before binding is complete, the lifecycle
    // methods are dropped on the floor. Due to this, we will send lifecycle methods that
    // may have happened since the bind began.
    State currentState = mLifecycleRegistry.getCurrentState();
    if (currentState.isAtLeast(State.STARTED)) {
      mCarAppBinding.dispatchAppLifecycleEvent(Event.ON_START);
      if (currentState.isAtLeast(State.RESUMED)) {
        mCarAppBinding.dispatchAppLifecycleEvent(Event.ON_RESUME);
      }
    }
  }

  @Override
  public void reportStatus(PrintWriter pw, Pii piiHandling) {
    pw.printf("- state: %s\n", mLifecycleRegistry.getCurrentState());
    pw.printf("- is valid: %b\n", mIsValid);

    if (mLastStartTimeMillis >= 0) {
      long durationMillis =
          mTemplateContext.getSystemClockWrapper().elapsedRealtime() - mLastStartTimeMillis;
      pw.printf("- duration: %s\n", StringUtils.formatDuration(durationMillis));
    }

    mCarAppBinding.reportStatus(pw, piiHandling);
    mTemplateContext.reportStatus(pw);

    for (Map.Entry<String, Host> entry : mHostServices.entrySet()) {
      pw.printf("\nHost service: %s\n", entry.getKey());
      entry.getValue().reportStatus(pw, piiHandling);
    }
  }

  @Override
  public Lifecycle getLifecycle() {
    // Don't assert whether the object is valid here, since callers may use the lifecycle to
    // know.
    return mLifecycleRegistry;
  }

  /**
   * Returns the stub for the {@link ICarHost} binder that apps use to communicate with this host.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public ICarHost.Stub getCarHostStub() {
    return mCarHostStub;
  }

  void onNewIntentDispatched() {
    // Don't assert whether the object is valid here as this is an asynchronous API and could be
    // called after being invalidated we don't want to cause a crash after the previous
    // shutdown.
    if (!mIsValid) {
      return;
    }

    for (Host host : mHostServices.values()) {
      host.onNewIntentDispatched();
    }
  }

  private void onConfigurationChanged() {
    if (mCarAppBinding.isBound()) {
      mCarAppBinding.dispatch(
          CarContext.CAR_SERVICE,
          NamedAppServiceCall.create(
              CarAppApi.ON_CONFIGURATION_CHANGED,
              (ICarApp carApp, ANRToken anrToken) ->
                  carApp.onConfigurationChanged(
                      mTemplateContext.getResources().getConfiguration(),
                      new OnDoneCallbackStub(mTemplateContext, anrToken))));
    }
  }

  @SuppressWarnings("nullness")
  private CarHost(TemplateContext templateContext) {
    mTemplateContext = templateContext;
    mCarAppBinding =
        CarAppBinding.create(
            templateContext,
            mCarHostStub,
            new CarAppBindingCallback() {
              @Override
              public void onCarAppBound() {
                onAppBound();
              }

              @Override
              public void onNewIntentDispatched() {
                CarHost.this.onNewIntentDispatched();
              }

              @Override
              public void onCarAppUnbound() {
                mIsAppBound = false;
                templateContext.getAppBindingStateProvider().updateAppBindingState(mIsAppBound);
                templateContext.getEventManager().dispatchEvent(EventType.APP_UNBOUND);
              }
            });

    templateContext
        .getEventManager()
        .subscribeEvent(this, EventType.CONFIGURATION_CHANGED, this::onConfigurationChanged);

    mTelemetryHandler = templateContext.getTelemetryHandler();

    mLifecycleRegistry.handleLifecycleEvent(Event.ON_CREATE);

    mLifecycleRegistry.addObserver(
        new DefaultLifecycleObserver() {
          @Override
          public void onStart(LifecycleOwner lifecycleOwner) {
            mLastStartTimeMillis = templateContext.getSystemClockWrapper().elapsedRealtime();
            dispatch(Event.ON_START);
          }

          @Override
          public void onResume(LifecycleOwner lifecycleOwner) {
            dispatch(Event.ON_RESUME);
          }

          @Override
          public void onPause(LifecycleOwner lifecycleOwner) {
            dispatch(Event.ON_PAUSE);
          }

          @Override
          public void onStop(LifecycleOwner lifecycleOwner) {
            dispatch(Event.ON_STOP);
            long durationMillis =
                templateContext.getSystemClockWrapper().elapsedRealtime() - mLastStartTimeMillis;
            if (mLastStartTimeMillis < 0 || durationMillis < 0) {
              L.w(
                  LogTags.APP_HOST,
                  "Negative duration %d or negative last start time %d",
                  durationMillis,
                  mLastStartTimeMillis);
              return;
            }
            mTelemetryHandler.logCarAppTelemetry(
                TelemetryEvent.newBuilder(UiAction.APP_RUNTIME, mCarAppBinding.getAppName())
                    .setDurationMs(durationMillis));
            mLastStartTimeMillis = -1;
          }

          private void dispatch(Event event) {
            if (mCarAppBinding.isBound()) {
              mCarAppBinding.dispatchAppLifecycleEvent(event);
            }
          }
        });
  }

  private void assertIsValid() {
    Preconditions.checkState(mIsValid, "Accessed the car host after it became invalidated");
  }

  private final class CarHostStubImpl extends ICarHost.Stub {
    @Override
    public void startCarApp(Intent intent) {
      mTemplateContext.getCarAppManager().startCarApp(intent);
    }

    @Override
    public void finish() {
      mTemplateContext.getCarAppManager().finishCarApp();
    }

    @Override
    public IBinder getHost(String type) {
      assertIsValid();
      Host service = mHostServices.get(type);
      if (CarContext.NAVIGATION_SERVICE.equals(type)
          && !mTemplateContext.getCarAppPackageInfo().isNavigationApp()) {
        throw new IllegalArgumentException(
            "Attempted to retrieve the navigation service, but the app is not a"
                + " navigation app");
      } else if (CarContext.CONSTRAINT_SERVICE.equals(type)
          && mTemplateContext.getCarHostConfig().getNegotiatedApi() < CarAppApiLevels.LEVEL_2) {
        throw new IllegalArgumentException(
            "Attempted to retrieve the constraint service, but the host's API level is"
                + " less than "
                + CarAppApiLevels.LEVEL_2);
      } else if (CarContext.HARDWARE_SERVICE.equals(type)
          && mTemplateContext.getCarHostConfig().getNegotiatedApi() < CarAppApiLevels.LEVEL_3) {
        throw new IllegalArgumentException(
            "Attempted to retrieve the hardware service, but the host's API level is"
                + " less than "
                + CarAppApiLevels.LEVEL_3);
      }

      if (service != null) {
        return service.getBinder();
      }

      throw new IllegalArgumentException("Unknown host service type:" + type);
    }
  }
}
