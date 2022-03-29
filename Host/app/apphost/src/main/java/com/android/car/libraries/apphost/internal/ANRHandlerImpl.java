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

import static com.android.car.libraries.apphost.common.EventManager.EventType.APP_DISCONNECTED;
import static com.android.car.libraries.apphost.common.EventManager.EventType.APP_UNBOUND;

import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.car.libraries.apphost.common.ANRHandler;
import com.android.car.libraries.apphost.common.CarAppError;
import com.android.car.libraries.apphost.common.ErrorHandler;
import com.android.car.libraries.apphost.common.EventManager;
import com.android.car.libraries.apphost.logging.CarAppApi;
import com.android.car.libraries.apphost.logging.CarAppApiErrorType;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryHandler;

/** Implementation of an {@link ANRHandler}. */
public class ANRHandlerImpl implements ANRHandler {
  private final Handler mHandler = new Handler(Looper.getMainLooper(), new HandlerCallback());
  private final ComponentName mAppName;
  private final TelemetryHandler mTelemetryhandler;
  private final ErrorHandler mErrorHandler;

  /** Creates an {@link ANRHandler} */
  public static ANRHandler create(
      ComponentName appName,
      ErrorHandler errorHandler,
      TelemetryHandler telemetryHandler,
      EventManager eventManager) {
    return new ANRHandlerImpl(appName, errorHandler, telemetryHandler, eventManager);
  }

  /**
   * Performs the call and checks for application not responding.
   *
   * <p>The ANR check will happen in {@link #ANR_TIMEOUT_MS} milliseconds after calling {@link
   * ANRCheckingCall#call}.
   */
  @Override
  public void callWithANRCheck(CarAppApi carAppApi, ANRCheckingCall call) {
    enqueueANRCheck(carAppApi);
    call.call(
        new ANRToken() {
          @Override
          public void dismiss() {
            mHandler.removeMessages(carAppApi.ordinal());
          }

          @Override
          public CarAppApi getCarAppApi() {
            return carAppApi;
          }
        });
  }

  private void enqueueANRCheck(CarAppApi carAppApi) {
    mHandler.removeMessages(carAppApi.ordinal());
    mHandler.sendMessageDelayed(mHandler.obtainMessage(carAppApi.ordinal()), ANR_TIMEOUT_MS);
  }

  private void onWaitClicked(CarAppApi carAppApi) {
    enqueueANRCheck(carAppApi);
    mErrorHandler.showError(
        CarAppError.builder(mAppName).setType(CarAppError.Type.ANR_WAITING).build());
  }

  private void removeAllANRChecks() {
    for (CarAppApi api : CarAppApi.values()) {
      mHandler.removeMessages(api.ordinal());
    }
  }

  @SuppressWarnings("nullness")
  private ANRHandlerImpl(
      ComponentName appName,
      ErrorHandler errorHandler,
      TelemetryHandler telemetryHandler,
      EventManager eventManager) {
    mAppName = appName;
    mErrorHandler = errorHandler;
    mTelemetryhandler = telemetryHandler;

    // Remove any outstanding ANR check whenever the app becomes unbound or crashes.
    eventManager.subscribeEvent(this, APP_UNBOUND, this::removeAllANRChecks);
    eventManager.subscribeEvent(this, APP_DISCONNECTED, this::removeAllANRChecks);
  }

  /** A {@link Handler.Callback} used to implement unbinding. */
  private class HandlerCallback implements Handler.Callback {
    @Override
    public boolean handleMessage(Message msg) {
      final CarAppApi carAppApi = CarAppApi.values()[msg.what];
      if (carAppApi == CarAppApi.UNKNOWN_API) {
        L.w(LogTags.APP_HOST, "Unexpected message for handler %s", msg);
        return false;
      } else {
        // Show an ANR screen allowing the user to wait.
        // If the user wants to wait, we will show a waiting screen that still allows EXIT.
        mTelemetryhandler.logCarAppApiFailureTelemetry(mAppName, carAppApi, CarAppApiErrorType.ANR);

        mErrorHandler.showError(
            CarAppError.builder(mAppName)
                .setType(CarAppError.Type.ANR_TIMEOUT)
                .setDebugMessage("ANR API: " + carAppApi.name())
                .setExtraAction(() -> onWaitClicked(carAppApi))
                .build());

        return true;
      }
    }
  }
}
