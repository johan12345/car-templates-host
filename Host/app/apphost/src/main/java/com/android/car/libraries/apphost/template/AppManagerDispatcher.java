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
package com.android.car.libraries.apphost.template;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.car.app.CarContext;
import androidx.car.app.IAppManager;
import com.android.car.libraries.apphost.ManagerDispatcher;
import com.android.car.libraries.apphost.common.AppServiceCall;
import com.android.car.libraries.apphost.common.NamedAppServiceCall;
import com.android.car.libraries.apphost.common.OnDoneCallbackStub;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.CarAppApi;

/** Dispatcher of calls to the {@link IAppManager}. */
public class AppManagerDispatcher extends ManagerDispatcher<IAppManager> {
  /** Creates an instance of {@link AppManagerDispatcher} with the given app binding object. */
  public static AppManagerDispatcher create(Object appBinding) {
    return new AppManagerDispatcher(appBinding);
  }

  /** Dispatches {@link IAppManager#getTemplate} to the app. */
  @AnyThread
  public void dispatchGetTemplate(AppServiceCall<IAppManager> getTemplateCall) {
    dispatch(NamedAppServiceCall.create(CarAppApi.GET_TEMPLATE, getTemplateCall));
  }

  /** Dispatches {@link IAppManager#onBackPressed} to the app. */
  @AnyThread
  public void dispatchOnBackPressed(TemplateContext templateContext) {
    dispatch(
        NamedAppServiceCall.create(
            CarAppApi.ON_BACK_PRESSED,
            (manager, anrToken) ->
                manager.onBackPressed(new OnDoneCallbackStub(templateContext, anrToken))));
  }

  /** Dispatches {@link IAppManager#startLocationUpdates} to the app. */
  @MainThread
  public void dispatchStartLocationUpdates(TemplateContext templateContext) {
    dispatch(
        NamedAppServiceCall.create(
            CarAppApi.START_LOCATION_UPDATES,
            (manager, anrToken) ->
                manager.startLocationUpdates(new OnDoneCallbackStub(templateContext, anrToken))));
  }

  /** Dispatches {@link IAppManager#stopLocationUpdates} to the app. */
  @MainThread
  public void dispatchStopLocationUpdates(TemplateContext templateContext) {
    dispatch(
        NamedAppServiceCall.create(
            CarAppApi.STOP_LOCATION_UPDATES,
            (manager, anrToken) ->
                manager.stopLocationUpdates(new OnDoneCallbackStub(templateContext, anrToken))));
  }

  private AppManagerDispatcher(Object appBinding) {
    super(CarContext.APP_SERVICE, appBinding);
  }
}
