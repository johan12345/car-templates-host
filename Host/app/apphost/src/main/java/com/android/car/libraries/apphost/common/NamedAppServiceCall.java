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

import android.os.RemoteException;
import com.android.car.libraries.apphost.logging.CarAppApi;

/**
 * A {@link AppServiceCall} decorated with a name, useful for logging.
 *
 * @param <ServiceT> the type of service to make the call for.
 */
public class NamedAppServiceCall<ServiceT> implements AppServiceCall<ServiceT> {
  private final AppServiceCall<ServiceT> mCall;
  private final CarAppApi mCarAppApi;

  /** Creates an instance of a {@link NamedAppServiceCall} for the given API. */
  public static <ServiceT> NamedAppServiceCall<ServiceT> create(
      CarAppApi carAppApi, AppServiceCall<ServiceT> call) {
    return new NamedAppServiceCall<>(carAppApi, call);
  }

  /** Returns the API this call is made for. */
  public CarAppApi getCarAppApi() {
    return mCarAppApi;
  }

  @Override
  public void dispatch(ServiceT appService, ANRHandler.ANRToken anrToken) throws RemoteException {
    mCall.dispatch(appService, anrToken);
  }

  @Override
  public String toString() {
    return "[" + mCarAppApi.name() + "]";
  }

  private NamedAppServiceCall(CarAppApi carAppApi, AppServiceCall<ServiceT> call) {
    mCall = call;
    mCarAppApi = carAppApi;
  }
}
