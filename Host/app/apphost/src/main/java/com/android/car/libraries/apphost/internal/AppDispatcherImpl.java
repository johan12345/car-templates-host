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

import static com.android.car.libraries.apphost.logging.TelemetryHandler.getErrorType;

import android.content.ComponentName;
import android.graphics.Rect;
import android.os.RemoteException;
import androidx.car.app.FailureResponse;
import androidx.car.app.ISurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.InputCallbackDelegate;
import androidx.car.app.model.OnCheckedChangeDelegate;
import androidx.car.app.model.OnClickDelegate;
import androidx.car.app.model.OnContentRefreshDelegate;
import androidx.car.app.model.OnItemVisibilityChangedDelegate;
import androidx.car.app.model.OnSelectedDelegate;
import androidx.car.app.model.SearchCallbackDelegate;
import androidx.car.app.navigation.model.PanModeDelegate;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import com.android.car.libraries.apphost.common.ANRHandler;
import com.android.car.libraries.apphost.common.AppBindingStateProvider;
import com.android.car.libraries.apphost.common.AppDispatcher;
import com.android.car.libraries.apphost.common.CarAppError;
import com.android.car.libraries.apphost.common.ErrorHandler;
import com.android.car.libraries.apphost.common.OnDoneCallbackStub;
import com.android.car.libraries.apphost.common.OneWayIPC;
import com.android.car.libraries.apphost.internal.BlockingOneWayIPC.BlockingResponse;
import com.android.car.libraries.apphost.logging.CarAppApi;
import com.android.car.libraries.apphost.logging.L;
import com.android.car.libraries.apphost.logging.LogTags;
import com.android.car.libraries.apphost.logging.TelemetryHandler;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class to set up safe remote callbacks to apps.
 *
 * <p>App interfaces to client are {@code oneway} so the calling thread does not block waiting for a
 * response.
 */
public class AppDispatcherImpl implements AppDispatcher {
  /** A request to send over the wire to the app that does not wait for a ANR check. */
  private interface OneWayIPCNoANRCheck {
    void send() throws RemoteException;
  }

  private final ComponentName mAppName;
  private final ErrorHandler mErrorHandler;
  private final ANRHandler mANRHandler;
  private final TelemetryHandler mTelemetryHandler;
  private final AppBindingStateProvider mAppBindingStateProvider;

  /** Creates an {@link AppDispatcher} instance for an app. */
  public static AppDispatcher create(
      ComponentName appName,
      ErrorHandler errorHandler,
      ANRHandler anrHandler,
      TelemetryHandler telemetryHandler,
      AppBindingStateProvider appBindingStateProvider) {
    return new AppDispatcherImpl(
        appName, errorHandler, anrHandler, telemetryHandler, appBindingStateProvider);
  }

  @Override
  public void dispatchSurfaceAvailable(
      ISurfaceCallback surfaceListener, SurfaceContainer surfaceContainer) {
    dispatch(
        anrToken ->
            surfaceListener.onSurfaceAvailable(
                Bundleable.create(surfaceContainer),
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_SURFACE_AVAILABLE);
  }

  @Override
  public void dispatchSurfaceDestroyed(
      ISurfaceCallback surfaceListener, SurfaceContainer surfaceContainer) {
    // onSurfaceDestroyed is called blocking since the OS expects that whenever we return
    // the call we are done using the Surface.
    BlockingResponse<Void> blockingResponse = new BlockingResponse<>();
    OneWayIPC ipc =
        anrToken ->
            surfaceListener.onSurfaceDestroyed(
                Bundleable.create(surfaceContainer),
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider) {
                  @Override
                  public void onSuccess(@Nullable Bundleable response) {
                    blockingResponse.setResponse(null);
                    super.onSuccess(response);
                  }

                  @Override
                  public void onFailure(Bundleable failureResponse) {
                    blockingResponse.setResponse(null);
                    super.onFailure(failureResponse);
                  }
                });

    dispatch(new BlockingOneWayIPC<>(ipc, blockingResponse), CarAppApi.ON_SURFACE_DESTROYED);
  }

  @Override
  public void dispatchVisibleAreaChanged(ISurfaceCallback surfaceListener, Rect visibleArea) {
    dispatch(
        anrToken ->
            surfaceListener.onVisibleAreaChanged(
                visibleArea,
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_VISIBLE_AREA_CHANGED);
  }

  @Override
  public void dispatchStableAreaChanged(ISurfaceCallback surfaceListener, Rect stableArea) {
    dispatch(
        anrToken ->
            surfaceListener.onStableAreaChanged(
                stableArea,
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_STABLE_AREA_CHANGED);
  }

  @Override
  public void dispatchOnSurfaceScroll(
      ISurfaceCallback surfaceListener, float distanceX, float distanceY) {
    dispatchNoANRCheck(() -> surfaceListener.onScroll(distanceX, distanceY), "onSurfaceScroll");
  }

  @Override
  public void dispatchOnSurfaceFling(
      ISurfaceCallback surfaceListener, float velocityX, float velocityY) {
    dispatchNoANRCheck(() -> surfaceListener.onFling(velocityX, velocityY), "onSurfaceFling");
  }

  @Override
  public void dispatchOnSurfaceScale(
      ISurfaceCallback surfaceListener, float focusX, float focusY, float scaleFactor) {
    dispatchNoANRCheck(
        () -> surfaceListener.onScale(focusX, focusY, scaleFactor), "onSurfaceScale");
  }

  @Override
  public void dispatchSearchTextChanged(
      SearchCallbackDelegate searchCallbackDelegate, String searchText) {
    dispatch(
        anrToken ->
            searchCallbackDelegate.sendSearchTextChanged(
                searchText,
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_SEARCH_TEXT_CHANGED);
  }

  @Override
  public void dispatchInputTextChanged(
      InputCallbackDelegate inputCallbackDelegate, String inputText) {
    dispatch(
        anrToken ->
            inputCallbackDelegate.sendInputTextChanged(
                inputText,
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_INPUT_TEXT_CHANGED);
  }

  @Override
  public void dispatchInputSubmitted(
      InputCallbackDelegate inputCallbackDelegate, String inputText) {
    dispatch(
        anrToken ->
            inputCallbackDelegate.sendInputSubmitted(
                inputText,
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_INPUT_SUBMITTED);
  }

  @Override
  public void dispatchSearchSubmitted(
      SearchCallbackDelegate searchCallbackDelegate, String searchText) {
    dispatch(
        anrToken ->
            searchCallbackDelegate.sendSearchSubmitted(
                searchText,
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_SEARCH_SUBMITTED);
  }

  @Override
  public void dispatchItemVisibilityChanged(
      OnItemVisibilityChangedDelegate onItemVisibilityChangedDelegate,
      int startIndexInclusive,
      int endIndexExclusive) {
    dispatch(
        anrToken ->
            onItemVisibilityChangedDelegate.sendItemVisibilityChanged(
                startIndexInclusive,
                endIndexExclusive,
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_ITEM_VISIBILITY_CHANGED);
  }

  @Override
  public void dispatchSelected(OnSelectedDelegate onSelectedDelegate, int index) {
    dispatch(
        anrToken ->
            onSelectedDelegate.sendSelected(
                index,
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_SELECTED);
  }

  @Override
  public void dispatchCheckedChanged(
      OnCheckedChangeDelegate onCheckedChangeDelegate, boolean isChecked) {
    dispatch(
        anrToken ->
            onCheckedChangeDelegate.sendCheckedChange(
                isChecked,
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_CHECKED_CHANGED);
  }

  @Override
  public void dispatchPanModeChanged(PanModeDelegate panModeDelegate, boolean isChecked) {
    dispatch(
        anrToken ->
            panModeDelegate.sendPanModeChanged(
                isChecked,
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_PAN_MODE_CHANGED);
  }

  @Override
  public void dispatchClick(OnClickDelegate onClickDelegate) {
    dispatch(
        anrToken ->
            onClickDelegate.sendClick(
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_CLICK);
  }

  @Override
  public void dispatchContentRefreshRequest(OnContentRefreshDelegate onContentRefreshDelegate) {
    dispatch(
        anrToken ->
            onContentRefreshDelegate.sendContentRefreshRequested(
                new OnDoneCallbackStub(
                    mErrorHandler,
                    mAppName,
                    anrToken,
                    mTelemetryHandler,
                    mAppBindingStateProvider)),
        CarAppApi.ON_CLICK);
  }

  /** Dispatches the given IPC call without checking for an ANR. */
  private void dispatchNoANRCheck(OneWayIPCNoANRCheck ipc, String callName) {
    try {
      ipc.send();
    } catch (RemoteException e) {
      mErrorHandler.showError(
          CarAppError.builder(mAppName)
              .setCause(e)
              .setDebugMessage("Remote call " + callName + " failed.")
              .build());
    }
  }

  @Override
  public void dispatch(OneWayIPC ipc, CarAppApi carAppApi) {
    dispatch(ipc, mErrorHandler::showError, carAppApi);
  }

  @Override
  public void dispatch(OneWayIPC ipc, ExceptionHandler exceptionHandler, CarAppApi carAppApi) {
    L.d(LogTags.APP_HOST, "Dispatching call %s", carAppApi.name());

    mANRHandler.callWithANRCheck(
        carAppApi,
        anrToken -> {
          try {
            ipc.send(anrToken);
          } catch (RemoteException | BundlerException | RuntimeException e) {
            mTelemetryHandler.logCarAppApiFailureTelemetry(
                mAppName, carAppApi, getErrorType(new FailureResponse(e)));

            exceptionHandler.handle(
                CarAppError.builder(mAppName)
                    .setCause(e)
                    .setDebugMessage("Remote call " + carAppApi.name() + " failed.")
                    .build());
          }
        });
  }

  private AppDispatcherImpl(
      ComponentName appName,
      ErrorHandler errorHandler,
      ANRHandler anrHandler,
      TelemetryHandler telemetryHandler,
      AppBindingStateProvider appBindingStateProvider) {
    mAppName = appName;
    mErrorHandler = errorHandler;
    mANRHandler = anrHandler;
    mTelemetryHandler = telemetryHandler;
    mAppBindingStateProvider = appBindingStateProvider;
  }
}
