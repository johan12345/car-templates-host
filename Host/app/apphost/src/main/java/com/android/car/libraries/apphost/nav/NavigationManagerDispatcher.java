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

import androidx.annotation.AnyThread;
import androidx.car.app.CarContext;
import androidx.car.app.navigation.INavigationManager;
import com.android.car.libraries.apphost.ManagerDispatcher;
import com.android.car.libraries.apphost.common.NamedAppServiceCall;
import com.android.car.libraries.apphost.common.OnDoneCallbackStub;
import com.android.car.libraries.apphost.common.TemplateContext;
import com.android.car.libraries.apphost.logging.CarAppApi;

/** Dispatcher of calls to the {@link INavigationManager}. */
public class NavigationManagerDispatcher extends ManagerDispatcher<INavigationManager> {
  /** Creates an instance of {@link NavigationManagerDispatcher}. */
  public static NavigationManagerDispatcher create(Object appBinding) {
    return new NavigationManagerDispatcher(appBinding);
  }

  /** Dispatches {@link INavigationManager#onStopNavigation} to the app. */
  @AnyThread
  public void dispatchStopNavigation(TemplateContext templateContext) {
    dispatch(
        NamedAppServiceCall.create(
            CarAppApi.STOP_NAVIGATION,
            (manager, anrToken) ->
                manager.onStopNavigation(new OnDoneCallbackStub(templateContext, anrToken))));
  }

  private NavigationManagerDispatcher(Object appBinding) {
    super(CarContext.NAVIGATION_SERVICE, appBinding);
  }
}
