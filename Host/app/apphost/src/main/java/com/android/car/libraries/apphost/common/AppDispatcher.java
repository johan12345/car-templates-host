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

import android.graphics.Rect;
import android.os.RemoteException;
import androidx.car.app.ISurfaceCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.InputCallbackDelegate;
import androidx.car.app.model.OnCheckedChangeDelegate;
import androidx.car.app.model.OnClickDelegate;
import androidx.car.app.model.OnContentRefreshDelegate;
import androidx.car.app.model.OnItemVisibilityChangedDelegate;
import androidx.car.app.model.OnSelectedDelegate;
import androidx.car.app.model.SearchCallbackDelegate;
import androidx.car.app.navigation.model.PanModeDelegate;
import androidx.car.app.serialization.BundlerException;
import com.android.car.libraries.apphost.logging.CarAppApi;

/**
 * Class to set up safe remote callbacks to apps.
 *
 * <p>App interfaces to client are {@code oneway} so the calling thread does not block waiting for a
 * response. (see go/aidl-best-practices for more information).
 */
public interface AppDispatcher {
  /**
   * Dispatches a {@link ISurfaceCallback#onSurfaceAvailable} to the provided listener with the
   * provided container.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchSurfaceAvailable(
      ISurfaceCallback surfaceListener, SurfaceContainer surfaceContainer);

  /**
   * Dispatches a {@link ISurfaceCallback#onSurfaceDestroyed} to the provided listener with the
   * provided container.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchSurfaceDestroyed(
      ISurfaceCallback surfaceListener, SurfaceContainer surfaceContainer);

  /**
   * Dispatches a {@link ISurfaceCallback#onVisibleAreaChanged} to the provided listener with the
   * provided area.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchVisibleAreaChanged(ISurfaceCallback surfaceListener, Rect visibleArea);

  /**
   * Dispatches a {@link ISurfaceCallback#onStableAreaChanged} to the provided listener with the
   * provided area.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchStableAreaChanged(ISurfaceCallback surfaceListener, Rect stableArea);

  /**
   * Dispatches a {@link ISurfaceCallback#onScroll} to the provided listener with the provided
   * scroll distance.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchOnSurfaceScroll(ISurfaceCallback surfaceListener, float distanceX, float distanceY);

  /**
   * Dispatches a {@link ISurfaceCallback#onFling} to the provided listener with the provided fling
   * velocity.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchOnSurfaceFling(ISurfaceCallback surfaceListener, float velocityX, float velocityY);

  /**
   * Dispatches a {@link ISurfaceCallback#onScale} to the provided listener with the provided focal
   * point and scale factor.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchOnSurfaceScale(
      ISurfaceCallback surfaceListener, float focusX, float focusY, float scaleFactor);

  /**
   * Dispatches a {@link SearchCallbackDelegate#sendSearchTextChanged} to the provided listener with
   * the provided search text.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchSearchTextChanged(SearchCallbackDelegate searchCallbackDelegate, String searchText);

  /**
   * Dispatches a {@link SearchCallbackDelegate#sendSearchSubmitted} to the provided listener with
   * the provided search text.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchSearchSubmitted(SearchCallbackDelegate searchCallbackDelegate, String searchText);

  /**
   * Dispatches an {@link InputCallbackDelegate#sendInputTextChanged} to the provided listener with
   * the provided input text.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchInputTextChanged(InputCallbackDelegate inputCallbackDelegate, String inputText);

  /**
   * Dispatches an {@link InputCallbackDelegate#sendInputSubmitted} to the provided listener with
   * the provided input text.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchInputSubmitted(InputCallbackDelegate inputCallbackDelegate, String inputText);

  /**
   * Dispatches a {@link OnItemVisibilityChangedDelegate#sendItemVisibilityChanged} to the provided
   * listener.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchItemVisibilityChanged(
      OnItemVisibilityChangedDelegate onItemVisibilityChangedDelegate,
      int startIndexInclusive,
      int endIndexExclusive);

  /**
   * Dispatches a {@link OnSelectedDelegate#sendSelected} to the provided listener.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchSelected(OnSelectedDelegate onSelectedDelegate, int index);

  /**
   * Dispatches a {@link OnCheckedChangeDelegate#sendCheckedChange} to the provided listener.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchCheckedChanged(OnCheckedChangeDelegate onCheckedChangeDelegate, boolean isChecked);

  /**
   * Dispatches a {@link PanModeDelegate#sendPanModeChanged(boolean, OnDoneCallback)} to the
   * provided listener.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchPanModeChanged(PanModeDelegate panModeDelegate, boolean isChecked);

  /**
   * Dispatches a {@link OnClickDelegate#sendClick} to the provided listener.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchClick(OnClickDelegate onClickDelegate);

  /**
   * Dispatches a {@link OnContentRefreshDelegate#sendContentRefreshRequested} event.
   *
   * @see #dispatch(OneWayIPC, CarAppApi) for information on error handling
   */
  void dispatchContentRefreshRequest(OnContentRefreshDelegate onContentRefreshDelegate);

  /**
   * Performs the IPC.
   *
   * <p>The calls are oneway. Given this any exception thrown by the client will not reach us, they
   * will be in their own process. (see go/aidl-best-practices for more information).
   *
   * <p>This method will handle app exceptions (described below) as well as {@link BundlerException}
   * which would be thrown if the host fails to bundle an object before sending it over (should
   * never happen).
   *
   * <h1>App Exceptions</h1>
   *
   * <p>Here are the possible exceptions thrown by the app, and when they may happen.
   *
   * <dl>
   *   <dt>{@link RemoteException}
   *   <dd>This exception is thrown when the binder is dead (i.e. the app crashed).
   *   <dt>{@link RuntimeException}
   *   <dd>The should not happen in regular scenario. The only cases where may happen are if the app
   *       is running in the same process as the host, or if the IPC was wrongly configured to not
   *       be {@code oneway}.
   * </dl>
   *
   * <p>The following are the types of {@link RuntimeException} that the binder let's through. See
   * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/os/Parcel.java;l=2061-2094
   *
   * <ul>
   *   <li>{@link SecurityException}
   *   <li>{@link android.os.BadParcelableException}
   *   <li>{@link IllegalArgumentException}
   *   <li>{@link NullPointerException}
   *   <li>{@link IllegalStateException}
   *   <li>{@link android.os.NetworkOnMainThreadException}
   *   <li>{@link UnsupportedOperationException}
   *   <li>{@link android.os.ServiceSpecificException}
   *   <li>{@link RuntimeException} - for any other exceptions.
   * </ul>
   */
  void dispatch(OneWayIPC ipc, CarAppApi carAppApi);

  /**
   * Performs the IPC allowing caller to define behavior for handling any exceptions.
   *
   * @see #dispatch(OneWayIPC, CarAppApi)
   */
  void dispatch(OneWayIPC ipc, ExceptionHandler exceptionHandler, CarAppApi carAppApi);

  /** Will handle exceptions received while performing a {@link OneWayIPC}. */
  interface ExceptionHandler {
    void handle(CarAppError carAppError);
  }
}
